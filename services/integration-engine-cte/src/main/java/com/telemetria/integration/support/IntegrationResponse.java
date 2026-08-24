package com.telemetria.integration.support;

import java.util.Map;

/** Resposta normalizada sem assumir o formato proprietário de cada provedor. */
public record IntegrationResponse(boolean success, int statusCode, String message, Map<String, Object> data) {
    public IntegrationResponse {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
