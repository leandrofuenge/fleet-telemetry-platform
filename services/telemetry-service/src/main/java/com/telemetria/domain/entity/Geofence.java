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
@Entity
@Table(name = "geofences", indexes = {
        @Index(name = "idx_gf_tenant", columnList = "tenant_id"),
        @Index(name = "idx_gf_ativo", columnList = "ativo")
})
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
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
    private Boolean aplicaTodos = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "veiculos_uuid", columnDefinition = "json")
    private List<String> veiculosUuid;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Geofence() {
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public static final class Builder {
        private Long id;
        private String uuid;
        private boolean uuidInformado;
        private Long tenantId;
        private String nome;
        private TipoGeofence tipo;
        private Double latitudeCentro;
        private Double longitudeCentro;
        private Double raio;
        private List<CoordenadasDto> vertices;
        private TipoAlertaGeofence tipoAlerta;
        private Boolean aplicaTodos;
        private boolean aplicaTodosInformado;
        private List<String> veiculosUuid;
        private Boolean ativo;
        private boolean ativoInformado;
        private LocalDateTime criadoEm;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            this.uuidInformado = true;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public Builder tipo(TipoGeofence tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder latitudeCentro(Double latitudeCentro) {
            this.latitudeCentro = latitudeCentro;
            return this;
        }

        public Builder longitudeCentro(Double longitudeCentro) {
            this.longitudeCentro = longitudeCentro;
            return this;
        }

        public Builder raio(Double raio) {
            this.raio = raio;
            return this;
        }

        public Builder vertices(List<CoordenadasDto> vertices) {
            this.vertices = vertices;
            return this;
        }

        public Builder tipoAlerta(TipoAlertaGeofence tipoAlerta) {
            this.tipoAlerta = tipoAlerta;
            return this;
        }

        public Builder aplicaTodos(Boolean aplicaTodos) {
            this.aplicaTodos = aplicaTodos;
            this.aplicaTodosInformado = true;
            return this;
        }

        public Builder veiculosUuid(List<String> veiculosUuid) {
            this.veiculosUuid = veiculosUuid;
            return this;
        }

        public Builder ativo(Boolean ativo) {
            this.ativo = ativo;
            this.ativoInformado = true;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Geofence build() {
            Geofence geofence = new Geofence();
            geofence.id = id;
            if (uuidInformado) {
                geofence.uuid = uuid;
            }
            geofence.tenantId = tenantId;
            geofence.nome = nome;
            geofence.tipo = tipo;
            geofence.latitudeCentro = latitudeCentro;
            geofence.longitudeCentro = longitudeCentro;
            geofence.raio = raio;
            geofence.vertices = vertices;
            geofence.tipoAlerta = tipoAlerta;
            if (aplicaTodosInformado) {
                geofence.aplicaTodos = aplicaTodos;
            }
            geofence.veiculosUuid = veiculosUuid;
            if (ativoInformado) {
                geofence.ativo = ativo;
            }
            geofence.criadoEm = criadoEm;
            return geofence;
        }
    }

    // ========== ENUMS INTERNOS ==========
    public enum TipoGeofence {
        CIRCULO, POLIGONO
    }

    public enum TipoAlertaGeofence {
        ENTRADA, SAIDA, AMBOS
    }

    // ========== DTO PARA VÉRTICES (com getters e setters) ==========
    public static class CoordenadasDto {
        private Double lat;
        private Double lng;

        public CoordenadasDto() {
        }

        public CoordenadasDto(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }

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
