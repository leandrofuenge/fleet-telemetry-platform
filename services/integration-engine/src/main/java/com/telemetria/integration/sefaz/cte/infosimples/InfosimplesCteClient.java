package com.telemetria.integration.sefaz.cte.infosimples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component("infosimplesCteClient")
public class InfosimplesCteClient {

    private final RestTemplate restTemplate;

    @Value("${infosimples.url-base:https://api.infosimples.com/v2/consultas/receita-federal/cte}")
    private String urlBase;

    @Value("${infosimples.token:}")
    private String apiToken;

    public InfosimplesCteClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InfosimplesCteResponse consultarCteCompleto(InfosimplesCteRequest request) {
        String endpoint = urlBase + "?token=" + apiToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<InfosimplesCteRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<InfosimplesCteResponse> response = restTemplate.postForEntity(endpoint, entity, InfosimplesCteResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Erro HTTP na consulta Infosimples CT-e: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Falha na comunicação com a API Infosimples: " + e.getMessage(), e);
        }
    }
}
