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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "veiculos_cache", indexes = {
        @Index(name = "idx_vc_tenant", columnList = "tenant_id"),
        @Index(name = "idx_vc_device", columnList = "device_id"),
        @Index(name = "idx_vc_placa", columnList = "placa")
})
@Getter
@Setter
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
}
