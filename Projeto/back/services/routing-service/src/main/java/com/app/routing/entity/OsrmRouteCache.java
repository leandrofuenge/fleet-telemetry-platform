package com.app.routing.entity;

import com.app.routing.enums.PerfilOsrm;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "osrm_route_cache", indexes = {
        @Index(name = "idx_osrm_key", columnList = "cache_key"),
        @Index(name = "idx_osrm_expira", columnList = "expira_em")
})
public class OsrmRouteCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true, length = 64)
    private String cacheKey;

    @Column(name = "perfil", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PerfilOsrm perfil;

    @Column(name = "lat_origem", nullable = false)
    private Double latOrigem;

    @Column(name = "lng_origem", nullable = false)
    private Double lngOrigem;

    @Column(name = "lat_destino", nullable = false)
    private Double latDestino;

    @Column(name = "lng_destino", nullable = false)
    private Double lngDestino;

    @Column(name = "distancia_km", nullable = false)
    private Double distanciaKm;

    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @Column(name = "polyline", columnDefinition = "TEXT")
    private String polyline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geojson", columnDefinition = "json")
    private Object geojson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instrucoes", columnDefinition = "json")
    private Object instrucoes;

    @Column(name = "hits", nullable = false)
    private Integer hits;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public OsrmRouteCache() {
        this.perfil = PerfilOsrm.CAMINHAO;
        this.hits = 1;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private OsrmRouteCache(Long id, String cacheKey, PerfilOsrm perfil,
            Double latOrigem, Double lngOrigem,
            Double latDestino, Double lngDestino,
            Double distanciaKm, Integer duracaoMin,
            String polyline, Object geojson, Object instrucoes,
            Integer hits, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.id = id;
        this.cacheKey = cacheKey;
        this.perfil = perfil != null ? perfil : PerfilOsrm.CAMINHAO;
        this.latOrigem = latOrigem;
        this.lngOrigem = lngOrigem;
        this.latDestino = latDestino;
        this.lngDestino = lngDestino;
        this.distanciaKm = distanciaKm;
        this.duracaoMin = duracaoMin;
        this.polyline = polyline;
        this.geojson = geojson;
        this.instrucoes = instrucoes;
        this.hits = hits != null ? hits : 1;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    // ==================== GETTERS E SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public PerfilOsrm getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilOsrm perfil) {
        this.perfil = perfil;
    }

    public Double getLatOrigem() {
        return latOrigem;
    }

    public void setLatOrigem(Double latOrigem) {
        this.latOrigem = latOrigem;
    }

    public Double getLngOrigem() {
        return lngOrigem;
    }

    public void setLngOrigem(Double lngOrigem) {
        this.lngOrigem = lngOrigem;
    }

    public Double getLatDestino() {
        return latDestino;
    }

    public void setLatDestino(Double latDestino) {
        this.latDestino = latDestino;
    }

    public Double getLngDestino() {
        return lngDestino;
    }

    public void setLngDestino(Double lngDestino) {
        this.lngDestino = lngDestino;
    }

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public Integer getDuracaoMin() {
        return duracaoMin;
    }

    public void setDuracaoMin(Integer duracaoMin) {
        this.duracaoMin = duracaoMin;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public Object getGeojson() {
        return geojson;
    }

    public void setGeojson(Object geojson) {
        this.geojson = geojson;
    }

    public Object getInstrucoes() {
        return instrucoes;
    }

    public void setInstrucoes(Object instrucoes) {
        this.instrucoes = instrucoes;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    // Não criar setter para criadoEm porque é @CreationTimestamp e updatable=false
    // (pode ser deixado, mas geralmente não se modifica manualmente)
    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String cacheKey;
        private PerfilOsrm perfil = PerfilOsrm.CAMINHAO; // valor padrão
        private Double latOrigem;
        private Double lngOrigem;
        private Double latDestino;
        private Double lngDestino;
        private Double distanciaKm;
        private Integer duracaoMin;
        private String polyline;
        private Object geojson;
        private Object instrucoes;
        private Integer hits = 1; // valor padrão
        private LocalDateTime criadoEm;
        private LocalDateTime expiraEm;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder perfil(PerfilOsrm perfil) {
            this.perfil = perfil;
            return this;
        }

        public Builder latOrigem(Double latOrigem) {
            this.latOrigem = latOrigem;
            return this;
        }

        public Builder lngOrigem(Double lngOrigem) {
            this.lngOrigem = lngOrigem;
            return this;
        }

        public Builder latDestino(Double latDestino) {
            this.latDestino = latDestino;
            return this;
        }

        public Builder lngDestino(Double lngDestino) {
            this.lngDestino = lngDestino;
            return this;
        }

        public Builder distanciaKm(Double distanciaKm) {
            this.distanciaKm = distanciaKm;
            return this;
        }

        public Builder duracaoMin(Integer duracaoMin) {
            this.duracaoMin = duracaoMin;
            return this;
        }

        public Builder polyline(String polyline) {
            this.polyline = polyline;
            return this;
        }

        public Builder geojson(Object geojson) {
            this.geojson = geojson;
            return this;
        }

        public Builder instrucoes(Object instrucoes) {
            this.instrucoes = instrucoes;
            return this;
        }

        public Builder hits(Integer hits) {
            this.hits = hits;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder expiraEm(LocalDateTime expiraEm) {
            this.expiraEm = expiraEm;
            return this;
        }

        public OsrmRouteCache build() {
            return new OsrmRouteCache(
                    id, cacheKey, perfil,
                    latOrigem, lngOrigem,
                    latDestino, lngDestino,
                    distanciaKm, duracaoMin,
                    polyline, geojson, instrucoes,
                    hits, criadoEm, expiraEm);
        }
    }
}