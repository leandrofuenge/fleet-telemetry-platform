package com.telemetria.application.service;

public record TelemetriaProcessingResult(
        Status status,
        Long telemetriaId,
        String eventId,
        Long veiculoId,
        boolean outOfOrder,
        long sequenceGap) {

    public enum Status {
        PERSISTED,
        DUPLICATE_EVENT,
        DUPLICATE_SEQUENCE
    }

    public boolean duplicate() {
        return status != Status.PERSISTED;
    }
}
