package com.telemetria.integration.telemetry;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Registro idempotente do recebimento de um evento do telemetry-service. */
@Entity
@Table(name = "telemetria_integration_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_telemetria_integration_event", columnNames = "event_id") })
public class ProcessamentoEventoTelemetria {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(name = "outbox_event_id", nullable = false, length = 120)
    private String outboxEventId;

    @Column(name = "telemetria_id", nullable = false)
    private Long telemetriaId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "recebido_em", nullable = false, updatable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    protected ProcessamentoEventoTelemetria() {
    }

    public static ProcessamentoEventoTelemetria recebido(EventoTelemetriaPersistida event) {
        ProcessamentoEventoTelemetria receipt = new ProcessamentoEventoTelemetria();
        receipt.id = UUID.randomUUID();
        receipt.eventId = event.eventId();
        receipt.outboxEventId = event.outboxEventId();
        receipt.telemetriaId = event.telemetriaId();
        receipt.tenantId = event.tenantId();
        receipt.veiculoId = event.veiculoId();
        receipt.status = "RECEBIDO";
        receipt.recebidoEm = LocalDateTime.now();
        return receipt;
    }

    public void marcarProcessado() {
        this.status = "PROCESSADO";
        this.processadoEm = LocalDateTime.now();
    }
}
