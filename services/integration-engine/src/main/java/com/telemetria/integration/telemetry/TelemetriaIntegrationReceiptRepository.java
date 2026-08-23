package com.telemetria.integration.telemetry;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetriaIntegrationReceiptRepository extends JpaRepository<TelemetriaIntegrationReceipt, UUID> {
    boolean existsByEventId(String eventId);
}
