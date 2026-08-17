package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Evento persistido na mesma transação da telemetria. O publicador somente o
 * remove do estado PENDENTE depois que o broker confirmar o envio.
 */
@Entity
@Table(name = "telemetria_outbox", indexes = {
        @Index(name = "idx_outbox_status_retry", columnList = "status, proxima_tentativa_em"),
        @Index(name = "idx_outbox_veiculo", columnList = "veiculo_id, criado_em")
})
public class TelemetriaOutboxEvent {

    public static final String STATUS_PENDING = "PENDENTE";
    public static final String STATUS_PUBLISHED = "PUBLICADO";
    public static final String STATUS_FAILED = "FALHOU";

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "telemetria_id", nullable = false, unique = true)
    private Long telemetriaId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "publicado_em")
    private LocalDateTime publicadoEm;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private LocalDateTime proximaTentativaEm;

    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    public TelemetriaOutboxEvent() {
    }

    public static TelemetriaOutboxEvent pending(Telemetria telemetria, String payload) {
        TelemetriaOutboxEvent event = new TelemetriaOutboxEvent();
        event.id = UUID.randomUUID().toString();
        event.telemetriaId = telemetria.getId();
        event.tenantId = telemetria.getTenantId();
        event.veiculoId = telemetria.getVeiculoId();
        event.eventId = telemetria.getEventId();
        event.eventType = "TELEMETRIA_PERSISTIDA";
        event.payload = payload;
        event.status = STATUS_PENDING;
        event.tentativas = 0;
        event.criadoEm = LocalDateTime.now();
        event.proximaTentativaEm = event.criadoEm;
        return event;
    }

    public void markPublished() {
        this.status = STATUS_PUBLISHED;
        this.publicadoEm = LocalDateTime.now();
        this.ultimoErro = null;
    }

    public void scheduleRetry(String error, int maxAttempts, long initialDelayMillis) {
        this.tentativas++;
        this.ultimoErro = abbreviate(error, 2000);
        if (this.tentativas >= maxAttempts) {
            this.status = STATUS_FAILED;
            return;
        }
        long multiplier = 1L << Math.min(this.tentativas - 1, 10);
        this.proximaTentativaEm = LocalDateTime.now().plusNanos(initialDelayMillis * multiplier * 1_000_000L);
    }

    private static String abbreviate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getTelemetriaId() { return telemetriaId; }
    public void setTelemetriaId(Long telemetriaId) { this.telemetriaId = telemetriaId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTentativas() { return tentativas; }
    public void setTentativas(Integer tentativas) { this.tentativas = tentativas; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(LocalDateTime publicadoEm) { this.publicadoEm = publicadoEm; }
    public LocalDateTime getProximaTentativaEm() { return proximaTentativaEm; }
    public void setProximaTentativaEm(LocalDateTime proximaTentativaEm) { this.proximaTentativaEm = proximaTentativaEm; }
    public String getUltimoErro() { return ultimoErro; }
    public void setUltimoErro(String ultimoErro) { this.ultimoErro = ultimoErro; }
}
