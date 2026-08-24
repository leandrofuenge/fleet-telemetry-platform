package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.telemetria.domain.enums.StatusPontoEntrega;
import com.telemetria.domain.enums.TipoPontoEntrega;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * RF11 — Pontos de Entrega e Proof of Delivery
 * Entidade que representa um ponto de entrega/coleta na rota
 */
@Entity
@Table(name = "pontos_entrega", indexes = {
        @Index(name = "idx_pe_viagem", columnList = "viagem_id"),
        @Index(name = "idx_pe_rota", columnList = "rota_id"),
        @Index(name = "idx_pe_status", columnList = "status"),
        @Index(name = "idx_pe_tipo", columnList = "tipo"),
        @Index(name = "idx_pe_ordem", columnList = "ordem")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PontoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;

    @Column(name = "rota_id", nullable = false)
    private Long rotaId;

    @Column(name = "ordem", nullable = false)
    private Integer ordem; // Ordem de execução na rota

    // ── Tipo e Status ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPontoEntrega tipo; // COLETA, ENTREGA, PARADA, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPontoEntrega status = StatusPontoEntrega.PENDENTE;

    // ── Dados do Destinatário/Local ──────────────────────────

    @Column(name = "nome_destinatario", length = 255)
    private String nomeDestinatario;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "raio_metros", nullable = false)
    private Integer raioMetros = 50; // Raio para validar chegada

    // ── RN-ENT-001: Proof of Delivery ───────────────────────

    /**
     * RF11: Caminho da assinatura no MinIO (nunca BLOB no banco)
     * Obrigatório para tipo ENTREGA ao marcar ENTREGUE
     */
    @Column(name = "assinatura_path", length = 500)
    private String assinaturaPath;

    /**
     * RF11: Caminho da foto de entrega no MinIO (nunca BLOB no banco)
     * Obrigatório para tipo ENTREGA ao marcar ENTREGUE (se não houver assinatura)
     */
    @Column(name = "foto_entrega_path", length = 500)
    private String fotoEntregaPath;

    /**
     * RF11: Ocorrência preenchida quando status = FALHOU
     * Campo obrigatório para justificar falha na entrega
     */
    @Column(name = "ocorrencia", length = 1000)
    private String ocorrencia;

    // ── Timestamps de Execução ───────────────────────────────

    @Column(name = "data_chegada")
    private LocalDateTime dataChegada;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @Column(name = "tempo_permanencia_min")
    private Integer tempoPermanenciaMin;

    // ── Relacionamentos ───────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id", insertable = false, updatable = false)
    private Rota rota;

    // ── Auditoria ────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ================================
    // Construtores
    // ================================

    public PontoEntrega() {
    }

    // ================================
    // Getters e Setters
    // ================================

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

    public Long getRotaId() {
        return rotaId;
    }

    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public TipoPontoEntrega getTipo() {
        return tipo;
    }

    public void setTipo(TipoPontoEntrega tipo) {
        this.tipo = tipo;
    }

    public StatusPontoEntrega getStatus() {
        return status;
    }

    public void setStatus(StatusPontoEntrega status) {
        this.status = status;
    }

    public String getNomeDestinatario() {
        return nomeDestinatario;
    }

    public void setNomeDestinatario(String nomeDestinatario) {
        this.nomeDestinatario = nomeDestinatario;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getRaioMetros() {
        return raioMetros;
    }

    public void setRaioMetros(Integer raioMetros) {
        this.raioMetros = raioMetros;
    }

    // RN-ENT-001: Getters e Setters para Proof of Delivery

    public String getAssinaturaPath() {
        return assinaturaPath;
    }

    public void setAssinaturaPath(String assinaturaPath) {
        this.assinaturaPath = assinaturaPath;
    }

    public String getFotoEntregaPath() {
        return fotoEntregaPath;
    }

    public void setFotoEntregaPath(String fotoEntregaPath) {
        this.fotoEntregaPath = fotoEntregaPath;
    }

    public String getOcorrencia() {
        return ocorrencia;
    }

    public void setOcorrencia(String ocorrencia) {
        this.ocorrencia = ocorrencia;
    }

    public LocalDateTime getDataChegada() {
        return dataChegada;
    }

    public void setDataChegada(LocalDateTime dataChegada) {
        this.dataChegada = dataChegada;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDateTime dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public Integer getTempoPermanenciaMin() {
        return tempoPermanenciaMin;
    }

    public void setTempoPermanenciaMin(Integer tempoPermanenciaMin) {
        this.tempoPermanenciaMin = tempoPermanenciaMin;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    // ================================
    // Builder
    // ================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long viagemId;
        private Long rotaId;
        private Integer ordem;
        private TipoPontoEntrega tipo;
        private StatusPontoEntrega status = StatusPontoEntrega.PENDENTE;
        private String nomeDestinatario;
        private String endereco;
        private Double latitude;
        private Double longitude;
        private Integer raioMetros = 50;
        private String assinaturaPath;
        private String fotoEntregaPath;
        private String ocorrencia;
        private LocalDateTime dataChegada;
        private LocalDateTime dataEntrega;
        private Integer tempoPermanenciaMin;

        Builder() {
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

        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }

        public Builder ordem(Integer ordem) {
            this.ordem = ordem;
            return this;
        }

        public Builder tipo(TipoPontoEntrega tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder status(StatusPontoEntrega status) {
            this.status = status;
            return this;
        }

        public Builder nomeDestinatario(String nomeDestinatario) {
            this.nomeDestinatario = nomeDestinatario;
            return this;
        }

        public Builder endereco(String endereco) {
            this.endereco = endereco;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder raioMetros(Integer raioMetros) {
            this.raioMetros = raioMetros;
            return this;
        }

        public Builder assinaturaPath(String assinaturaPath) {
            this.assinaturaPath = assinaturaPath;
            return this;
        }

        public Builder fotoEntregaPath(String fotoEntregaPath) {
            this.fotoEntregaPath = fotoEntregaPath;
            return this;
        }

        public Builder ocorrencia(String ocorrencia) {
            this.ocorrencia = ocorrencia;
            return this;
        }

        public Builder dataChegada(LocalDateTime dataChegada) {
            this.dataChegada = dataChegada;
            return this;
        }

        public Builder dataEntrega(LocalDateTime dataEntrega) {
            this.dataEntrega = dataEntrega;
            return this;
        }

        public Builder tempoPermanenciaMin(Integer tempoPermanenciaMin) {
            this.tempoPermanenciaMin = tempoPermanenciaMin;
            return this;
        }

        public PontoEntrega build() {
            PontoEntrega pe = new PontoEntrega();
            pe.setId(this.id);
            pe.setTenantId(this.tenantId);
            pe.setViagemId(this.viagemId);
            pe.setRotaId(this.rotaId);
            pe.setOrdem(this.ordem);
            pe.setTipo(this.tipo);
            pe.setStatus(this.status);
            pe.setNomeDestinatario(this.nomeDestinatario);
            pe.setEndereco(this.endereco);
            pe.setLatitude(this.latitude);
            pe.setLongitude(this.longitude);
            pe.setRaioMetros(this.raioMetros);
            pe.setAssinaturaPath(this.assinaturaPath);
            pe.setFotoEntregaPath(this.fotoEntregaPath);
            pe.setOcorrencia(this.ocorrencia);
            pe.setDataChegada(this.dataChegada);
            pe.setDataEntrega(this.dataEntrega);
            pe.setTempoPermanenciaMin(this.tempoPermanenciaMin);
            return pe;
        }
    }

    // ================================
    // Métodos Utilitários
    // ================================

    /**
     * Verifica se é um ponto de entrega (tipo = ENTREGA)
     */
    public boolean isEntrega() {
        return tipo == TipoPontoEntrega.ENTREGA;
    }

    /**
     * Verifica se é um ponto de coleta (tipo = COLETA)
     */
    public boolean isColeta() {
        return tipo == TipoPontoEntrega.COLETA;
    }

    /**
     * Verifica se foi entregue (status = ENTREGUE)
     */
    public boolean isEntregue() {
        return status == StatusPontoEntrega.ENTREGUE;
    }

    /**
     * Verifica se falhou (status = FALHOU)
     */
    public boolean isFalhou() {
        return status == StatusPontoEntrega.FALHOU;
    }

    @Override
    public String toString() {
        return "PontoEntrega{" +
                "id=" + id +
                ", viagemId=" + viagemId +
                ", ordem=" + ordem +
                ", tipo=" + tipo +
                ", status=" + status +
                ", nomeDestinatario='" + nomeDestinatario + '\'' +
                ", assinaturaPath='" + assinaturaPath + '\'' +
                ", fotoEntregaPath='" + fotoEntregaPath + '\'' +
                '}';
    }
}
