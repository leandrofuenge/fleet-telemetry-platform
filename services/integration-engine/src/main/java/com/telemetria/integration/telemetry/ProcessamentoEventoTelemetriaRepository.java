package com.telemetria.integration.telemetry;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessamentoEventoTelemetriaRepository extends JpaRepository<ProcessamentoEventoTelemetria, UUID> {
    boolean existsByEventId(String eventId);
}
