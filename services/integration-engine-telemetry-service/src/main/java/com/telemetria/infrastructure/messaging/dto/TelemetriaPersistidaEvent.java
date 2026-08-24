package com.telemetria.infrastructure.messaging.dto;

import java.time.LocalDateTime;

public record TelemetriaPersistidaEvent(
        String outboxEventId,
        String eventId,
        Long telemetriaId,
        Long tenantId,
        Long veiculoId,
        LocalDateTime eventTime,
        LocalDateTime ingestionTime,
        String rawPayload) {
}
