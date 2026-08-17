package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

// ─────────────────────────────────────────────────────────────
// 1. VeiculoCache.java
// ─────────────────────────────────────────────────────────────
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "veiculos_cache", indexes = {
        @Index(name = "idx_vc_tenant", columnList = "tenant_id"),
        @Index(name = "idx_vc_device", columnList = "device_id"),
        @Index(name = "idx_vc_placa", columnList = "placa")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoCache {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id; // Mesmo ID do vehicle-service (sem auto-increment)

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "modelo", nullable = false)
    private String modelo;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "tipo_veiculo", nullable = false, length = 50)
    private String tipoVeiculo;

    @Column(name = "consumo_medio")
    private Double consumoMedio;

    @Column(name = "capacidade_carga_kg")
    private Double capacidadeCargaKg;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "device_imei", length = 20)
    private String deviceImei;

    @Column(name = "pbt_kg")
    private Double pbtKg;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @UpdateTimestamp
    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;

    // ==================== GETTERS ====================
    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getPlaca() {
        return placa;
    }

    public String getModelo() {
        return modelo;
    }

    public String getMarca() {
        return marca;
    }

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public Double getConsumoMedio() {
        return consumoMedio;
    }

    public Double getCapacidadeCargaKg() {
        return capacidadeCargaKg;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceImei() {
        return deviceImei;
    }

    public Double getPbtKg() {
        return pbtKg;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public LocalDateTime getSincronizadoEm() {
        return sincronizadoEm;
    }

    // ==================== SETTERS ====================
    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public void setTipoVeiculo(String tipoVeiculo) {
        this.tipoVeiculo = tipoVeiculo;
    }

    public void setConsumoMedio(Double consumoMedio) {
        this.consumoMedio = consumoMedio;
    }

    public void setCapacidadeCargaKg(Double capacidadeCargaKg) {
        this.capacidadeCargaKg = capacidadeCargaKg;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceImei(String deviceImei) {
        this.deviceImei = deviceImei;
    }

    public void setPbtKg(Double pbtKg) {
        this.pbtKg = pbtKg;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public void setSincronizadoEm(LocalDateTime sincronizadoEm) {
        this.sincronizadoEm = sincronizadoEm;
    }
}
