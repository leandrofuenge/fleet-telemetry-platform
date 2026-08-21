package com.telemetria.integration.sefaz.cte;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class CteHttpsSoapTransport implements CteSoapTransport {
    private final HttpClient httpClient;

    public CteHttpsSoapTransport(HttpClient httpClient) {
        this.httpClient = java.util.Objects.requireNonNull(httpClient);
    }

    @Override
    public CteSoapResponse send(CteSoapRequest request) {
        var builder = HttpRequest.newBuilder(request.endpoint())
                .timeout(request.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + request.soapAction() + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(request.envelope(), StandardCharsets.UTF_8));
        request.headers().forEach(builder::header);
        long started = System.nanoTime();
        try {
            var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new CteSoapResponse(response.statusCode(), response.body(), response.headers().map(),
                    Duration.ofNanos(System.nanoTime() - started));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CteTransportException("Envio SOAP CT-e interrompido.", exception);
        } catch (Exception exception) {
            throw new CteTransportException("Falha no transporte HTTPS/SOAP CT-e.", exception);
        }
    }
}
