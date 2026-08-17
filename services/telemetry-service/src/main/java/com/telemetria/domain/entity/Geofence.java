package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import lombok.NoArgsConstructor;

@Entity
@Table(name = "geofences", indexes = {
        @Index(name = "idx_gf_tenant", columnList = "tenant_id"),
        @Index(name = "idx_gf_ativo", columnList = "ativo")
})
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

    // ========== GETTERS E SETTERS MANUAIS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoGeofence getTipo() {
        return tipo;
    }

    public void setTipo(TipoGeofence tipo) {
        this.tipo = tipo;
    }

    public Double getLatitudeCentro() {
        return latitudeCentro;
    }

    public void setLatitudeCentro(Double latitudeCentro) {
        this.latitudeCentro = latitudeCentro;
    }

    public Double getLongitudeCentro() {
        return longitudeCentro;
    }

    public void setLongitudeCentro(Double longitudeCentro) {
        this.longitudeCentro = longitudeCentro;
    }

    public Double getRaio() {
        return raio;
    }

    public void setRaio(Double raio) {
        this.raio = raio;
    }

    public List<CoordenadasDto> getVertices() {
        return vertices;
    }

    public void setVertices(List<CoordenadasDto> vertices) {
        this.vertices = vertices;
    }

    public TipoAlertaGeofence getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(TipoAlertaGeofence tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public Boolean getAplicaTodos() {
        return aplicaTodos;
    }

    public void setAplicaTodos(Boolean aplicaTodos) {
        this.aplicaTodos = aplicaTodos;
    }

    public List<String> getVeiculosUuid() {
        return veiculosUuid;
    }

    public void setVeiculosUuid(List<String> veiculosUuid) {
        this.veiculosUuid = veiculosUuid;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    // ========== ENUMS INTERNOS ==========
    public enum TipoGeofence {
        CIRCULO, POLIGONO
    }

    public enum TipoAlertaGeofence {
        ENTRADA, SAIDA, AMBOS
    }

    // ========== DTO PARA VÉRTICES (com getters e setters) ==========
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoordenadasDto {
        private Double lat;
        private Double lng;

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }
}