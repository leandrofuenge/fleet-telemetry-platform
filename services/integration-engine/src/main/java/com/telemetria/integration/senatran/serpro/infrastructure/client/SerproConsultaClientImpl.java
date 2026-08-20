package com.telemetria.integration.senatran.serpro.infrastructure.client;
import com.telemetria.integration.senatran.serpro.application.SerproConsultaClient;
import com.telemetria.integration.senatran.serpro.domain.SerproIntegrationException;
import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;
import com.telemetria.integration.senatran.serpro.infrastructure.resilience.SerproResiliencePolicy;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoRequest;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoResponse;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component("serproConsultaClient")
public class SerproConsultaClientImpl implements SerproConsultaClient {
    private static final Logger log = LoggerFactory.getLogger(SerproConsultaClientImpl.class);
    private final RestTemplate restTemplate;
    private final SerproProperties properties;
    private final MeterRegistry meterRegistry;
    private final SerproResiliencePolicy resilience;

    public SerproConsultaClientImpl(RestTemplateBuilder builder, SerproProperties properties,
            MeterRegistry meterRegistry, SerproResiliencePolicy resilience) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.resilience = resilience;
        this.restTemplate = builder
                .setConnectTimeout(properties.getSerpro().getConnectTimeout())
                .setReadTimeout(properties.getSerpro().getReadTimeout()).build();
    }

    @Override
    public SerproVeiculoResponse consultarVeiculo(SerproVeiculoRequest request) {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            return SerproVeiculoResponse.erro("Token da integração SERPRO/RADAR não configurado.");
        }
        SerproResiliencePolicy.Decision decision = resilience.acquire();
        if (decision == SerproResiliencePolicy.Decision.RATE_LIMITED) {
            meterRegistry.counter("integration.serpro.rejected", "reason", "rate_limit").increment();
            return SerproVeiculoResponse.erro("Limite local de consultas SERPRO/RADAR atingido.");
        }
        if (decision == SerproResiliencePolicy.Decision.CIRCUIT_OPEN) {
            meterRegistry.counter("integration.serpro.rejected", "reason", "circuit_open").increment();
            return SerproVeiculoResponse.erro("Integração SERPRO/RADAR temporariamente suspensa após falhas consecutivas.");
        }
        URI endpoint = UriComponentsBuilder.fromUriString(properties.getUrlSerproRadar())
                .queryParam("token", properties.getToken()).build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxAttempts = properties.getSerpro().getMaxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                ResponseEntity<SerproApiResponse> response = restTemplate.postForEntity(
                        endpoint, new HttpEntity<>(request, headers), SerproApiResponse.class);
                sample.stop(timer("success"));
                resilience.success();
                return map(response.getBody());
            } catch (HttpStatusCodeException e) {
                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                sample.stop(timer(retryable ? "retryable_http_error" : "http_error"));
                if (retryable && attempt < maxAttempts) { retry(attempt, maxAttempts); continue; }
                if (retryable) resilience.failure();
                log.warn("Consulta SERPRO/RADAR falhou com status HTTP {}", e.getStatusCode().value());
                return SerproVeiculoResponse.erro("Erro HTTP " + e.getStatusCode().value()
                        + " na consulta SERPRO/RADAR.");
            } catch (ResourceAccessException e) {
                sample.stop(timer("communication_error"));
                if (attempt < maxAttempts) { retry(attempt, maxAttempts); continue; }
                resilience.failure();
                log.warn("Consulta SERPRO/RADAR indisponível após {} tentativa(s)", attempt);
                return SerproVeiculoResponse.erro("Tempo limite ou indisponibilidade na comunicação SERPRO/RADAR.");
            } catch (RuntimeException e) {
                sample.stop(timer("unexpected_error"));
                resilience.failure();
                log.error("Erro inesperado na consulta SERPRO/RADAR", e);
                return SerproVeiculoResponse.erro("Erro inesperado na integração SERPRO/RADAR.");
            }
        }
        return SerproVeiculoResponse.erro("Consulta SERPRO/RADAR não concluída.");
    }

    private SerproVeiculoResponse map(SerproApiResponse body) {
        if (body == null) return SerproVeiculoResponse.erro("Resposta vazia recebida do serviço SERPRO/RADAR.");
        boolean success = body.code() != null && body.code() == 200
                && (body.errors() == null || body.errors().isEmpty());
        List<SerproVeiculoResponse.VeiculoRadarData> data = body.data() == null ? List.of()
                : body.data().stream().map(vehicle -> new SerproVeiculoResponse.VeiculoRadarData(
                        vehicle.placa(), vehicle.uf(), vehicle.marcaModelo(),
                        vehicle.infracoes() == null ? List.of() : vehicle.infracoes().stream()
                                .map(i -> new SerproVeiculoResponse.InfracaoData(i.ait(), i.descricao(), i.data(),
                                        i.dataHora(), i.hora(), i.situacao(), i.autuacao(),
                                        i.autuacaoPdfUrl(), i.boletoPdfUrl())).toList())).toList();
        String error = success ? null : errorMessage(body);
        return new SerproVeiculoResponse(body.code(), body.codeMessage(), data,
                body.errors() == null ? List.of() : body.errors(), success, error);
    }

    private String errorMessage(SerproApiResponse response) {
        if (response.codeMessage() != null && !response.codeMessage().isBlank()) return response.codeMessage();
        if (response.errors() != null && !response.errors().isEmpty()) return String.join("; ", response.errors());
        return "Consulta SERPRO/RADAR não concluída com sucesso.";
    }

    private Timer timer(String outcome) {
        return Timer.builder("integration.serpro.request").description("Latência das consultas SERPRO/RADAR")
                .tag("outcome", outcome).register(meterRegistry);
    }

    private void retry(int attempt, int maxAttempts) {
        log.debug("Nova tentativa SERPRO/RADAR após falha transitória ({}/{})", attempt, maxAttempts);
        Duration delay = properties.getSerpro().getRetryDelay();
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(0, delay.toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SerproIntegrationException("Consulta SERPRO/RADAR interrompida.", e);
        }
    }
}
