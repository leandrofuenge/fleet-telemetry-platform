package com.telemetria.infrastructure.messaging.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record TelemetriaEnvelope(
        JsonNode json,
        String rawPayload,
        Long veiculoId,
        Long tenantIdInformado,
        String eventId,
        Long sequenceNumber) {
}
