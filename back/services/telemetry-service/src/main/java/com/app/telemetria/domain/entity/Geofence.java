package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// ─────────────────────────────────────────────────────────────
// 5. Geofence.java
// ─────────────────────────────────────────────────────────────
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "geofences", indexes = {
        @Index(name = "idx_gf_tenant", columnList = "tenant_id"),
        @Index(name = "idx_gf_ativo", columnList = "ativo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = java.util.UUID.randomUUID().toString();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "tipo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoGeofence tipo;

    @Column(name = "latitude_centro")
    private Double latitudeCentro;

    @Column(name = "longitude_centro")
    private Double longitudeCentro;

    @Column(name = "raio")
    private Double raio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vertices", columnDefinition = "json")
    private List<CoordenadasDto> vertices;

    @Column(name = "tipo_alerta", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoAlertaGeofence tipoAlerta;

    @Column(name = "aplica_todos", nullable = false)
    @Builder.Default
    private Boolean aplicaTodos = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "veiculos_uuid", columnDefinition = "json")
    private List<String> veiculosUuid;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // Enum interno
    public enum TipoGeofence {
        CIRCULO, POLIGONO
    }

    public enum TipoAlertaGeofence {
        ENTRADA, SAIDA, AMBOS
    }

    // DTO para vértices do polígono
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoordenadasDto {
        private Double lat;
        private Double lng;
    }
}
