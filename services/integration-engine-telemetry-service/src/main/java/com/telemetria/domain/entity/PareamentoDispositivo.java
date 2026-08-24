package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pareamentos_dispositivo", indexes = {
        @Index(name = "idx_pareamento_codigo", columnList = "codigo_hash", unique = true),
        @Index(name = "idx_pareamento_expira", columnList = "expira_em")
})
public class PareamentoDispositivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "consumido_em")
    private LocalDateTime consumidoEm;

    @Column(name = "device_id_consumidor", length = 64)
    private String deviceIdConsumidor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public String getCodigoHash() { return codigoHash; }
    public void setCodigoHash(String codigoHash) { this.codigoHash = codigoHash; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public LocalDateTime getConsumidoEm() { return consumidoEm; }
    public void setConsumidoEm(LocalDateTime consumidoEm) { this.consumidoEm = consumidoEm; }
    public String getDeviceIdConsumidor() { return deviceIdConsumidor; }
    public void setDeviceIdConsumidor(String deviceIdConsumidor) { this.deviceIdConsumidor = deviceIdConsumidor; }
}
