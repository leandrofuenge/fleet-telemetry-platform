package com.telemetria.integration.senatran.serpro;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component("serproConsultaClient")
public class SerproConsultaClientImpl implements SerproConsultaClient {

    private final RestTemplate restTemplate;

    @Value("${infosimples.url-serpro-radar:https://api.infosimples.com/v2/consultas/serpro/radar/veiculo}")
    private String baseUrl;

    @Value("${infosimples.token:}")
    private String apiToken;

    public SerproConsultaClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SerproVeiculoResponse consultarVeiculo(SerproVeiculoRequest request) {
        URI endpoint = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("token", apiToken)
                .build()
                .encode()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<SerproVeiculoResponse> response = restTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<>(request, headers),
                    SerproVeiculoResponse.class
            );
            SerproVeiculoResponse body = response.getBody();
            if (body == null) {
                return SerproVeiculoResponse.erro("Resposta com corpo vazio recebida do serviço SERPRO/RADAR.");
            }

            boolean sucesso = body.code() != null && body.code() == 200
                    && (body.errors() == null || body.errors().isEmpty());
            String mensagemErro = sucesso ? null : obterMensagemErro(body);
            return new SerproVeiculoResponse(
                    body.code(), body.codeMessage(), body.data(), body.errors(), sucesso, mensagemErro
            );
        } catch (HttpStatusCodeException e) {
            return SerproVeiculoResponse.erro(
                    "Erro HTTP %s na consulta SERPRO/RADAR: %s"
                            .formatted(e.getStatusCode(), e.getResponseBodyAsString())
            );
        } catch (Exception e) {
            return SerproVeiculoResponse.erro(
                    "Erro de comunicação com o serviço SERPRO/RADAR: " + e.getMessage()
            );
        }
    }

    private String obterMensagemErro(SerproVeiculoResponse response) {
        if (response.codeMessage() != null && !response.codeMessage().isBlank()) {
            return response.codeMessage();
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            return String.join("; ", response.errors());
        }
        return "Consulta SERPRO/RADAR não concluída com sucesso.";
    }
}
