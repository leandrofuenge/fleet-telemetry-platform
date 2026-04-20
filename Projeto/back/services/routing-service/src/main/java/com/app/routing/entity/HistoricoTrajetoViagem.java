package com.app.routing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "historico_trajeto_viagem", indexes = {
        @Index(name = "idx_htv_viagem", columnList = "viagem_id"),
        @Index(name = "idx_htv_segmento", columnList = "viagem_id, segmento")
})
public class HistoricoTrajetoViagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;

    @Column(name = "segmento", nullable = false)
    private Integer segmento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trajeto", nullable = false, columnDefinition = "json")
    private List<Map<String, Object>> trajeto; // [{lat, lng, ts, vel, ignicao}]

    @Column(name = "km_segmento", nullable = false)
    private Double kmSegmento;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public HistoricoTrajetoViagem() {
        this.kmSegmento = 0.0;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private HistoricoTrajetoViagem(Long id, Long tenantId, Long viagemId, Integer segmento,
            List<Map<String, Object>> trajeto, Double kmSegmento,
            LocalDateTime criadoEm, Viagem viagem) {
        this.id = id;
        this.tenantId = tenantId;
        this.viagemId = viagemId;
        this.segmento = segmento;
        this.trajeto = trajeto;
        this.kmSegmento = kmSegmento != null ? kmSegmento : 0.0;
        this.criadoEm = criadoEm;
        this.viagem = viagem;
    }

    // ==================== GETTERS E SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getViagemId() {
        return viagemId;
    }

    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }

    public Integer getSegmento() {
        return segmento;
    }

    public void setSegmento(Integer segmento) {
        this.segmento = segmento;
    }

    public List<Map<String, Object>> getTrajeto() {
        return trajeto;
    }

    public void setTrajeto(List<Map<String, Object>> trajeto) {
        this.trajeto = trajeto;
    }

    public Double getKmSegmento() {
        return kmSegmento;
    }

    public void setKmSegmento(Double kmSegmento) {
        this.kmSegmento = kmSegmento;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    // Não criar setter para criadoEm porque é @CreationTimestamp e updatable=false
    // (pode ser deixado, mas geralmente não se modifica manualmente)
    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Long tenantId;
        private Long viagemId;
        private Integer segmento;
        private List<Map<String, Object>> trajeto;
        private Double kmSegmento = 0.0; // valor padrão
        private LocalDateTime criadoEm;
        private Viagem viagem;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }

        public Builder segmento(Integer segmento) {
            this.segmento = segmento;
            return this;
        }

        public Builder trajeto(List<Map<String, Object>> trajeto) {
            this.trajeto = trajeto;
            return this;
        }

        public Builder kmSegmento(Double kmSegmento) {
            this.kmSegmento = kmSegmento;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder viagem(Viagem viagem) {
            this.viagem = viagem;
            return this;
        }

        public HistoricoTrajetoViagem build() {
            return new HistoricoTrajetoViagem(
                    id, tenantId, viagemId, segmento,
                    trajeto, kmSegmento, criadoEm, viagem);
        }
    }
}