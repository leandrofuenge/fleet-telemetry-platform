package com.telemetria.integration.antt.ciot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component("ciotClient")
public class CiotClientImpl implements CiotClient {

    private final RestTemplate restTemplate;

    @Value("${antt.ciot.url-base:https://api-homologacao.pef.com.br/v1/ciot}")
    private String baseUrl;

    @Value("${antt.ciot.token:}")
    private String authToken;

    public CiotClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CiotResponse gerarCiot(CiotRequest request) {
        String endpoint = baseUrl + "/emitir";

        HttpHeaders headers = criarHeaders();
        HttpEntity<CiotRequest> entity = new HttpEntity<>(request, headers);

        try {
            // Chamada REST POST convertendo JSON automaticamente
            ResponseEntity<CiotResponse> response = restTemplate.postForEntity(endpoint, entity, CiotResponse.class);
            return extrairCorpoOuErro(response, "Resposta com corpo vazio recebida da API de CIOT.");
            
        } catch (HttpStatusCodeException e) {
            // Captura erros HTTP 4xx e 5xx (ex: 400 Bad Request, 403 Forbidden, 500 Internal Server Error)
            return new CiotResponse(false, null, null, 
                String.format("Falha HTTP %s ao gerar CIOT: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            // Captura erros de rede, timeout, DNS ou conexão
            return new CiotResponse(false, null, null, "Erro de comunicação com o serviço de CIOT: " + e.getMessage());
        }
    }

    @Override
    public CiotResponse encerrarCiot(String numeroCiot) {
        String endpoint = baseUrl + "/encerrar/" + numeroCiot;

        HttpHeaders headers = criarHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CiotResponse> response = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, CiotResponse.class);
            return extrairCorpoOuErro(response, "Resposta com corpo vazio recebida no encerramento do CIOT.");
            
        } catch (HttpStatusCodeException e) {
            return new CiotResponse(false, null, null, 
                String.format("Falha HTTP %s ao encerrar CIOT: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            return new CiotResponse(false, null, null, "Erro de comunicação ao encerrar CIOT: " + e.getMessage());
        }
    }

    /**
     * Monta os cabeçalhos HTTP com Content-Type e Authorization formatado.
     */
    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String bearerToken = (authToken != null && authToken.startsWith("Bearer ")) 
                ? authToken 
                : "Bearer " + authToken;
                
        headers.set("Authorization", bearerToken);
        return headers;
    }

    /**
     * Garante que não retornaremos null caso a API responda HTTP 200/204 sem corpo.
     */
    private CiotResponse extrairCorpoOuErro(ResponseEntity<CiotResponse> response, String mensagemCorpoVazio) {
        if (response != null && response.getBody() != null) {
            return response.getBody();
        }
        return new CiotResponse(false, null, null, mensagemCorpoVazio);
    }
}
