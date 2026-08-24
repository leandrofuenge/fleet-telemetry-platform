package com.telemetria.integration.sefaz.nfe.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.nfe.NfeSefazUnavailableException;

/** Implementação HTTP/1.1, SOAP 1.2 e mTLS para a SEFAZ NF-e. */
@Component
public class NfeHttpsSoapTransport implements NfeSoapTransport {

    private final HttpClient httpClient;

    @Autowired
    public NfeHttpsSoapTransport(@Qualifier("sefazSslContext") SSLContext sslContext) {
        this(HttpClient.newBuilder().sslContext(sslContext).version(HttpClient.Version.HTTP_1_1).build());
    }

    NfeHttpsSoapTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String enviar(String envelopeSoap, URI endpoint, NfeSoapService service, int timeoutMillis) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new NfeSefazUnavailableException("Endpoint HTTPS NF-e não configurado.");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout NF-e deve ser maior que zero.");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(timeoutMillis))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + service.soapAction() + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelopeSoap, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NfeSefazUnavailableException("SEFAZ retornou HTTP " + response.statusCode()
                        + " para a operação " + service.soapAction() + ".");
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new NfeSefazUnavailableException("SEFAZ retornou HTTP " + response.statusCode() + " sem corpo.");
            }
            return response.body();
        } catch (NfeSefazUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NfeSefazUnavailableException("Falha na comunicação mTLS/SOAP com a SEFAZ NF-e.", exception);
        }
    }
}
