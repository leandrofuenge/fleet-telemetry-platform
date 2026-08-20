package com.telemetria.infrastructure.integration.engine;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.telemetria.infrastructure.integration.engine.dto.SerproVeiculoConsultaRequest;
import com.telemetria.infrastructure.integration.engine.dto.SerproVeiculoConsultaResponse;

/** Cliente interno para a consulta SENATRAN/SERPRO fornecida pelo integration-engine. */
@Component
public class SerproIntegrationClient {

    private static final String CONSULTA_PATH = "/api/integracoes/senatran/serpro/veiculos/consulta";
    private static final String API_KEY_HEADER = "X-Integration-API-Key";

    private final RestTemplate restTemplate;
    private final SerproIntegrationProperties properties;

    public SerproIntegrationClient(RestTemplateBuilder builder, SerproIntegrationProperties properties) {
        this.restTemplate = builder.build();
        this.properties = properties;
    }

    public SerproVeiculoConsultaResponse consultarVeiculo(SerproVeiculoConsultaRequest request,
            String correlationId) {
        if (!StringUtils.hasText(properties.getSerproApiKey())) {
            throw new SerproIntegrationUnavailableException("Chave interna da integração SERPRO não configurada.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(API_KEY_HEADER, properties.getSerproApiKey());
        if (StringUtils.hasText(correlationId)) headers.set("X-Correlation-ID", correlationId);

        try {
            ResponseEntity<SerproVeiculoConsultaResponse> response = restTemplate.postForEntity(
                    endpoint(), new HttpEntity<>(request, headers), SerproVeiculoConsultaResponse.class);
            if (response.getBody() == null) {
                throw new SerproIntegrationUnavailableException("Resposta vazia do integration-engine para SERPRO.");
            }
            return response.getBody();
        } catch (RestClientException exception) {
            throw new SerproIntegrationUnavailableException(
                    "Não foi possível consultar o SERPRO pelo integration-engine.", exception);
        }
    }

    private String endpoint() {
        return properties.getUrl().replaceAll("/+$", "") + CONSULTA_PATH;
    }

    public static class SerproIntegrationUnavailableException extends RuntimeException {
        public SerproIntegrationUnavailableException(String message) { super(message); }
        public SerproIntegrationUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
