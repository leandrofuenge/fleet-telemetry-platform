package com.telemetria.integration.antt.pisominimo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component("pisoMinimoClient")
public class PisoMinimoClientImpl implements PisoMinimoClient {

    private final RestTemplate restTemplate;

    @Value("${antt.piso-minimo.url-base:https://calculadorafrete.antt.gov.br/api/v1/calcular}")
    private String baseUrl;

    @Value("${antt.piso-minimo.token:}")
    private String authToken;

    public PisoMinimoClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PisoMinimoResponse calcularPisoMinimo(PisoMinimoRequest request) {
        HttpHeaders headers = criarHeaders();
        HttpEntity<PisoMinimoRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<PisoMinimoResponse> response = restTemplate.postForEntity(baseUrl, entity, PisoMinimoResponse.class);
            
            if (response.getBody() != null) {
                // Instancia uma cópia definindo sucesso = true
                PisoMinimoResponse body = response.getBody();
                return new PisoMinimoResponse(
                    body.valorTotal(), body.valorIda(), body.valorRetornoVazio(),
                    body.coeficienteCustoDeslocamento(), body.coeficienteCustoCargaDescarga(),
                    body.distancia(), body.operacaoTransporte(), body.normalizadoValorTotal(),
                    body.normalizadoValorIda(), body.normalizadoValorRetornoVazio(),
                    body.normalizadoCoeficienteCustoDeslocamento(), body.normalizadoCoeficienteCustoCargaDescarga(),
                    body.normalizadoDistancia(), true, null
                );
            }
            return PisoMinimoResponse.erro("Resposta com corpo vazio recebida da API de Piso Mínimo.");

        } catch (HttpStatusCodeException e) {
            return PisoMinimoResponse.erro(
                String.format("Erro HTTP %s na API Piso Mínimo: %s", e.getStatusCode(), e.getResponseBodyAsString())
            );
        } catch (Exception e) {
            return PisoMinimoResponse.erro("Falha de comunicação com a API Piso Mínimo: " + e.getMessage());
        }
    }

    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String bearerToken = (authToken != null && authToken.startsWith("Bearer ")) ? authToken : "Bearer " + authToken;
        headers.set("Authorization", bearerToken);
        return headers;
    }
}
