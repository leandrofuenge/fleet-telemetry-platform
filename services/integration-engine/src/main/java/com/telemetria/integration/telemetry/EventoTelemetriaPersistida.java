package com.telemetria.integration.telemetry;

import java.time.LocalDateTime;

/**
 * Contrato de integração publicado pelo telemetry-service no tópico telemetria-events.
 * O payload original é mantido apenas para que adaptadores futuros possam processá-lo.
 */
public record EventoTelemetriaPersistida(
        String outboxEventId,
        String eventId,
        Long telemetriaId,
        Long tenantId,
        Long veiculoId,
        LocalDateTime eventTime,
        LocalDateTime ingestionTime,
        String rawPayload) {

    public boolean valido() {
        return outboxEventId != null && !outboxEventId.isBlank()
                && eventId != null && !eventId.isBlank()
                && telemetriaId != null && tenantId != null && veiculoId != null;
    }
}
