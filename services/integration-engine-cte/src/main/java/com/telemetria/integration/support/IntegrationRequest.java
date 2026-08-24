package com.telemetria.integration.support;

import java.util.Map;

/** Requisição neutra usada pelos adaptadores cujos contratos externos são configuráveis. */
public record IntegrationRequest(String operation, Map<String, Object> data) {
    public IntegrationRequest {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("A operação da integração é obrigatória.");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
