package com.telemetria.integration.sefaz.cte;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record CteSoapRequest(URI endpoint, String soapAction, String envelope,
        Duration timeout, Map<String, String> headers) {
    public CteSoapRequest {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("O endpoint CT-e deve utilizar HTTPS.");
        }
        if (soapAction == null || soapAction.isBlank()) throw new IllegalArgumentException("SOAPAction é obrigatório.");
        if (envelope == null || envelope.isBlank()) throw new IllegalArgumentException("Envelope SOAP é obrigatório.");
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("Timeout deve ser positivo.");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
