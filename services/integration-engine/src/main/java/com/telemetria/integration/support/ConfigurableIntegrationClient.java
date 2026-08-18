package com.telemetria.integration.support;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/** Cliente HTTP base com autenticação, erros e resposta normalizados. */
public abstract class ConfigurableIntegrationClient {
    private final RestTemplate restTemplate;

    protected ConfigurableIntegrationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected IntegrationResponse post(String endpoint, String token, IntegrationRequest request) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Endpoint da integração não configurado.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Credencial da integração não configurada.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.replaceFirst("(?i)^Bearer\\s+", ""));
        try {
            ResponseEntity<IntegrationResponse> response = restTemplate.postForEntity(
                    URI.create(endpoint), new HttpEntity<>(request, headers), IntegrationResponse.class);
            if (response.getBody() == null) {
                return new IntegrationResponse(false, response.getStatusCode().value(), "Resposta vazia.", null);
            }
            return response.getBody();
        } catch (RestClientResponseException exception) {
            return new IntegrationResponse(false, exception.getStatusCode().value(),
                    "Erro HTTP na integração externa.", null);
        }
    }
}
