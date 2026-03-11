package com.app.routing.entity;

import com.app.routing.enums.StatusPontoEntrega;
import com.app.routing.enums.TipoPontoEntrega;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "pontos_entrega", indexes = {
        @Index(name = "idx_pe_viagem", columnList = "viagem_id"),
        @Index(name = "idx_pe_seq", columnList = "viagem_id, sequencia"),
        @Index(name = "idx_pe_status", columnList = "status")
})
public class PontoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;

    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @Column(name = "nome")
    private String nome;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "tipo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoPontoEntrega tipo;

    @Column(name = "janela_inicio")
    private LocalDateTime janelaInicio;

    @Column(name = "janela_fim")
    private LocalDateTime janelaFim;

    @Column(name = "eta_calculado")
    private LocalDateTime etaCalculado;

    @Column(name = "chegada_real")
    private LocalDateTime chegadaReal;

    @Column(name = "saida_real")
    private LocalDateTime saidaReal;

    @Column(name = "dentro_janela")
    private Boolean dentroJanela;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private StatusPontoEntrega status;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    @Column(name = "assinatura_path", length = 500)
    private String assinaturaPath;

    @Column(name = "foto_entrega_path", length = 500)
    private String fotoEntregaPath;

    @Column(name = "ocorrencia", columnDefinition = "TEXT")
    private String ocorrencia;

    @Column(name = "codigo_rastreio", length = 50)
    private String codigoRastreio;

    @Column(name = "nfe_chave", length = 50)
    private String nfeChave;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public PontoEntrega() {
        this.tipo = TipoPontoEntrega.ENTREGA;
        this.status = StatusPontoEntrega.PENDENTE;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private PontoEntrega(Long id, Long tenantId, Long viagemId, Integer sequencia,
            String nome, String endereco, Double latitude, Double longitude,
            TipoPontoEntrega tipo, LocalDateTime janelaInicio,
            LocalDateTime janelaFim, LocalDateTime etaCalculado,
            LocalDateTime chegadaReal, LocalDateTime saidaReal,
            Boolean dentroJanela, StatusPontoEntrega status,
            String destinatarioNome, String assinaturaPath,
            String fotoEntregaPath, String ocorrencia,
            String codigoRastreio, String nfeChave,
            LocalDateTime criadoEm, LocalDateTime atualizadoEm,
            Viagem viagem) {
        this.id = id;
        this.tenantId = tenantId;
        this.viagemId = viagemId;
        this.sequencia = sequencia;
        this.nome = nome;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipo = tipo != null ? tipo : TipoPontoEntrega.ENTREGA;
        this.janelaInicio = janelaInicio;
        this.janelaFim = janelaFim;
        this.etaCalculado = etaCalculado;
        this.chegadaReal = chegadaReal;
        this.saidaReal = saidaReal;
        this.dentroJanela = dentroJanela;
        this.status = status != null ? status : StatusPontoEntrega.PENDENTE;
        this.destinatarioNome = destinatarioNome;
        this.assinaturaPath = assinaturaPath;
        this.fotoEntregaPath = fotoEntregaPath;
        this.ocorrencia = ocorrencia;
        this.codigoRastreio = codigoRastreio;
        this.nfeChave = nfeChave;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
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

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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

    public TipoPontoEntrega getTipo() {
        return tipo;
    }

    public void setTipo(TipoPontoEntrega tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getJanelaInicio() {
        return janelaInicio;
    }

    public void setJanelaInicio(LocalDateTime janelaInicio) {
        this.janelaInicio = janelaInicio;
    }

    public LocalDateTime getJanelaFim() {
        return janelaFim;
    }

    public void setJanelaFim(LocalDateTime janelaFim) {
        this.janelaFim = janelaFim;
    }

    public LocalDateTime getEtaCalculado() {
        return etaCalculado;
    }

    public void setEtaCalculado(LocalDateTime etaCalculado) {
        this.etaCalculado = etaCalculado;
    }

    public LocalDateTime getChegadaReal() {
        return chegadaReal;
    }

    public void setChegadaReal(LocalDateTime chegadaReal) {
        this.chegadaReal = chegadaReal;
    }

    public LocalDateTime getSaidaReal() {
        return saidaReal;
    }

    public void setSaidaReal(LocalDateTime saidaReal) {
        this.saidaReal = saidaReal;
    }

    public Boolean getDentroJanela() {
        return dentroJanela;
    }

    public void setDentroJanela(Boolean dentroJanela) {
        this.dentroJanela = dentroJanela;
    }

    public StatusPontoEntrega getStatus() {
        return status;
    }

    public void setStatus(StatusPontoEntrega status) {
        this.status = status;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

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

    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    public void setCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }

    public String getNfeChave() {
        return nfeChave;
    }

    public void setNfeChave(String nfeChave) {
        this.nfeChave = nfeChave;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    // Nota: geralmente não se deve setar manualmente campos com @CreationTimestamp
    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
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
        private Integer sequencia;
        private String nome;
        private String endereco;
        private Double latitude;
        private Double longitude;
        private TipoPontoEntrega tipo = TipoPontoEntrega.ENTREGA; // valor padrão
        private LocalDateTime janelaInicio;
        private LocalDateTime janelaFim;
        private LocalDateTime etaCalculado;
        private LocalDateTime chegadaReal;
        private LocalDateTime saidaReal;
        private Boolean dentroJanela;
        private StatusPontoEntrega status = StatusPontoEntrega.PENDENTE; // valor padrão
        private String destinatarioNome;
        private String assinaturaPath;
        private String fotoEntregaPath;
        private String ocorrencia;
        private String codigoRastreio;
        private String nfeChave;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
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

        public Builder sequencia(Integer sequencia) {
            this.sequencia = sequencia;
            return this;
        }

        public Builder nome(String nome) {
            this.nome = nome;
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

        public Builder tipo(TipoPontoEntrega tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder janelaInicio(LocalDateTime janelaInicio) {
            this.janelaInicio = janelaInicio;
            return this;
        }

        public Builder janelaFim(LocalDateTime janelaFim) {
            this.janelaFim = janelaFim;
            return this;
        }

        public Builder etaCalculado(LocalDateTime etaCalculado) {
            this.etaCalculado = etaCalculado;
            return this;
        }

        public Builder chegadaReal(LocalDateTime chegadaReal) {
            this.chegadaReal = chegadaReal;
            return this;
        }

        public Builder saidaReal(LocalDateTime saidaReal) {
            this.saidaReal = saidaReal;
            return this;
        }

        public Builder dentroJanela(Boolean dentroJanela) {
            this.dentroJanela = dentroJanela;
            return this;
        }

        public Builder status(StatusPontoEntrega status) {
            this.status = status;
            return this;
        }

        public Builder destinatarioNome(String destinatarioNome) {
            this.destinatarioNome = destinatarioNome;
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

        public Builder codigoRastreio(String codigoRastreio) {
            this.codigoRastreio = codigoRastreio;
            return this;
        }

        public Builder nfeChave(String nfeChave) {
            this.nfeChave = nfeChave;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder atualizadoEm(LocalDateTime atualizadoEm) {
            this.atualizadoEm = atualizadoEm;
            return this;
        }

        public Builder viagem(Viagem viagem) {
            this.viagem = viagem;
            return this;
        }

        public PontoEntrega build() {
            return new PontoEntrega(
                    id, tenantId, viagemId, sequencia,
                    nome, endereco, latitude, longitude,
                    tipo, janelaInicio, janelaFim, etaCalculado,
                    chegadaReal, saidaReal, dentroJanela, status,
                    destinatarioNome, assinaturaPath, fotoEntregaPath,
                    ocorrencia, codigoRastreio, nfeChave,
                    criadoEm, atualizadoEm, viagem);
        }
    }
}