package com.app.routing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "relatorio_viagem", indexes = {
        @Index(name = "idx_rv_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rv_veiculo", columnList = "veiculo_uuid")
})
public class RelatorioViagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false, unique = true)
    private Long viagemId;

    @Column(name = "rota_id")
    private Long rotaId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "motorista_uuid", length = 36)
    private String motoristaUuid;

    // Aderência
    @Column(name = "aderencia_pct")
    private Double aderenciaPct;

    @Column(name = "desvios_count", nullable = false)
    private Integer desviosCount;

    @Column(name = "km_extras_total", nullable = false)
    private Double kmExtrasTotal;

    // Tempo
    @Column(name = "duracao_total_min")
    private Integer duracaoTotalMin;

    @Column(name = "atraso_chegada_min")
    private Integer atrasoChegadaMin;

    @Column(name = "tempo_paradas_min")
    private Integer tempoParadasMin;

    @Column(name = "tempo_ocioso_min")
    private Integer tempoOciosoMin;

    // Distância
    @Column(name = "km_total")
    private Double kmTotal;

    @Column(name = "km_planejado")
    private Double kmPlanejado;

    @Column(name = "diferenca_km")
    private Double diferencaKm;

    // Combustível
    @Column(name = "litros_consumidos")
    private Double litrosConsumidos;

    @Column(name = "custo_combustivel", precision = 10, scale = 2)
    private BigDecimal custoCombustivel;

    @Column(name = "consumo_medio")
    private Double consumoMedio;

    // Comportamento
    @Column(name = "score_final")
    private Integer scoreFinal;

    @Column(name = "frenagens_bruscas", nullable = false)
    private Integer frenagensBruscas;

    @Column(name = "excessos_velocidade", nullable = false)
    private Integer excessosVelocidade;

    @Column(name = "aceleracoes_bruscas", nullable = false)
    private Integer aceleracoesBruscas;

    // Entregas
    @Column(name = "entregas_total")
    private Integer entregasTotal;

    @Column(name = "entregas_sucesso")
    private Integer entregasSucesso;

    @Column(name = "entregas_falhas")
    private Integer entregasFalhas;

    @Column(name = "entregas_no_prazo")
    private Integer entregasNoPrazo;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @CreationTimestamp
    @Column(name = "gerado_em", nullable = false, updatable = false)
    private LocalDateTime geradoEm;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public RelatorioViagem() {
        this.desviosCount = 0;
        this.kmExtrasTotal = 0.0;
        this.frenagensBruscas = 0;
        this.excessosVelocidade = 0;
        this.aceleracoesBruscas = 0;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private RelatorioViagem(Long id, Long tenantId, Long viagemId, Long rotaId,
            String veiculoUuid, String motoristaUuid,
            Double aderenciaPct, Integer desviosCount, Double kmExtrasTotal,
            Integer duracaoTotalMin, Integer atrasoChegadaMin,
            Integer tempoParadasMin, Integer tempoOciosoMin,
            Double kmTotal, Double kmPlanejado, Double diferencaKm,
            Double litrosConsumidos, BigDecimal custoCombustivel, Double consumoMedio,
            Integer scoreFinal, Integer frenagensBruscas, Integer excessosVelocidade,
            Integer aceleracoesBruscas, Integer entregasTotal, Integer entregasSucesso,
            Integer entregasFalhas, Integer entregasNoPrazo, String pdfPath,
            LocalDateTime geradoEm, Viagem viagem) {
        this.id = id;
        this.tenantId = tenantId;
        this.viagemId = viagemId;
        this.rotaId = rotaId;
        this.veiculoUuid = veiculoUuid;
        this.motoristaUuid = motoristaUuid;
        this.aderenciaPct = aderenciaPct;
        this.desviosCount = desviosCount != null ? desviosCount : 0;
        this.kmExtrasTotal = kmExtrasTotal != null ? kmExtrasTotal : 0.0;
        this.duracaoTotalMin = duracaoTotalMin;
        this.atrasoChegadaMin = atrasoChegadaMin;
        this.tempoParadasMin = tempoParadasMin;
        this.tempoOciosoMin = tempoOciosoMin;
        this.kmTotal = kmTotal;
        this.kmPlanejado = kmPlanejado;
        this.diferencaKm = diferencaKm;
        this.litrosConsumidos = litrosConsumidos;
        this.custoCombustivel = custoCombustivel;
        this.consumoMedio = consumoMedio;
        this.scoreFinal = scoreFinal;
        this.frenagensBruscas = frenagensBruscas != null ? frenagensBruscas : 0;
        this.excessosVelocidade = excessosVelocidade != null ? excessosVelocidade : 0;
        this.aceleracoesBruscas = aceleracoesBruscas != null ? aceleracoesBruscas : 0;
        this.entregasTotal = entregasTotal;
        this.entregasSucesso = entregasSucesso;
        this.entregasFalhas = entregasFalhas;
        this.entregasNoPrazo = entregasNoPrazo;
        this.pdfPath = pdfPath;
        this.geradoEm = geradoEm;
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

    public Long getRotaId() {
        return rotaId;
    }

    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
    }

    public String getVeiculoUuid() {
        return veiculoUuid;
    }

    public void setVeiculoUuid(String veiculoUuid) {
        this.veiculoUuid = veiculoUuid;
    }

    public String getMotoristaUuid() {
        return motoristaUuid;
    }

    public void setMotoristaUuid(String motoristaUuid) {
        this.motoristaUuid = motoristaUuid;
    }

    public Double getAderenciaPct() {
        return aderenciaPct;
    }

    public void setAderenciaPct(Double aderenciaPct) {
        this.aderenciaPct = aderenciaPct;
    }

    public Integer getDesviosCount() {
        return desviosCount;
    }

    public void setDesviosCount(Integer desviosCount) {
        this.desviosCount = desviosCount;
    }

    public Double getKmExtrasTotal() {
        return kmExtrasTotal;
    }

    public void setKmExtrasTotal(Double kmExtrasTotal) {
        this.kmExtrasTotal = kmExtrasTotal;
    }

    public Integer getDuracaoTotalMin() {
        return duracaoTotalMin;
    }

    public void setDuracaoTotalMin(Integer duracaoTotalMin) {
        this.duracaoTotalMin = duracaoTotalMin;
    }

    public Integer getAtrasoChegadaMin() {
        return atrasoChegadaMin;
    }

    public void setAtrasoChegadaMin(Integer atrasoChegadaMin) {
        this.atrasoChegadaMin = atrasoChegadaMin;
    }

    public Integer getTempoParadasMin() {
        return tempoParadasMin;
    }

    public void setTempoParadasMin(Integer tempoParadasMin) {
        this.tempoParadasMin = tempoParadasMin;
    }

    public Integer getTempoOciosoMin() {
        return tempoOciosoMin;
    }

    public void setTempoOciosoMin(Integer tempoOciosoMin) {
        this.tempoOciosoMin = tempoOciosoMin;
    }

    public Double getKmTotal() {
        return kmTotal;
    }

    public void setKmTotal(Double kmTotal) {
        this.kmTotal = kmTotal;
    }

    public Double getKmPlanejado() {
        return kmPlanejado;
    }

    public void setKmPlanejado(Double kmPlanejado) {
        this.kmPlanejado = kmPlanejado;
    }

    public Double getDiferencaKm() {
        return diferencaKm;
    }

    public void setDiferencaKm(Double diferencaKm) {
        this.diferencaKm = diferencaKm;
    }

    public Double getLitrosConsumidos() {
        return litrosConsumidos;
    }

    public void setLitrosConsumidos(Double litrosConsumidos) {
        this.litrosConsumidos = litrosConsumidos;
    }

    public BigDecimal getCustoCombustivel() {
        return custoCombustivel;
    }

    public void setCustoCombustivel(BigDecimal custoCombustivel) {
        this.custoCombustivel = custoCombustivel;
    }

    public Double getConsumoMedio() {
        return consumoMedio;
    }

    public void setConsumoMedio(Double consumoMedio) {
        this.consumoMedio = consumoMedio;
    }

    public Integer getScoreFinal() {
        return scoreFinal;
    }

    public void setScoreFinal(Integer scoreFinal) {
        this.scoreFinal = scoreFinal;
    }

    public Integer getFrenagensBruscas() {
        return frenagensBruscas;
    }

    public void setFrenagensBruscas(Integer frenagensBruscas) {
        this.frenagensBruscas = frenagensBruscas;
    }

    public Integer getExcessosVelocidade() {
        return excessosVelocidade;
    }

    public void setExcessosVelocidade(Integer excessosVelocidade) {
        this.excessosVelocidade = excessosVelocidade;
    }

    public Integer getAceleracoesBruscas() {
        return aceleracoesBruscas;
    }

    public void setAceleracoesBruscas(Integer aceleracoesBruscas) {
        this.aceleracoesBruscas = aceleracoesBruscas;
    }

    public Integer getEntregasTotal() {
        return entregasTotal;
    }

    public void setEntregasTotal(Integer entregasTotal) {
        this.entregasTotal = entregasTotal;
    }

    public Integer getEntregasSucesso() {
        return entregasSucesso;
    }

    public void setEntregasSucesso(Integer entregasSucesso) {
        this.entregasSucesso = entregasSucesso;
    }

    public Integer getEntregasFalhas() {
        return entregasFalhas;
    }

    public void setEntregasFalhas(Integer entregasFalhas) {
        this.entregasFalhas = entregasFalhas;
    }

    public Integer getEntregasNoPrazo() {
        return entregasNoPrazo;
    }

    public void setEntregasNoPrazo(Integer entregasNoPrazo) {
        this.entregasNoPrazo = entregasNoPrazo;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public LocalDateTime getGeradoEm() {
        return geradoEm;
    }

    // Nota: campo com @CreationTimestamp não deve ser setado manualmente
    public void setGeradoEm(LocalDateTime geradoEm) {
        this.geradoEm = geradoEm;
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
        private Long rotaId;
        private String veiculoUuid;
        private String motoristaUuid;
        private Double aderenciaPct;
        private Integer desviosCount = 0; // valor padrão
        private Double kmExtrasTotal = 0.0; // valor padrão
        private Integer duracaoTotalMin;
        private Integer atrasoChegadaMin;
        private Integer tempoParadasMin;
        private Integer tempoOciosoMin;
        private Double kmTotal;
        private Double kmPlanejado;
        private Double diferencaKm;
        private Double litrosConsumidos;
        private BigDecimal custoCombustivel;
        private Double consumoMedio;
        private Integer scoreFinal;
        private Integer frenagensBruscas = 0; // valor padrão
        private Integer excessosVelocidade = 0; // valor padrão
        private Integer aceleracoesBruscas = 0; // valor padrão
        private Integer entregasTotal;
        private Integer entregasSucesso;
        private Integer entregasFalhas;
        private Integer entregasNoPrazo;
        private String pdfPath;
        private LocalDateTime geradoEm;
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

        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }

        public Builder veiculoUuid(String veiculoUuid) {
            this.veiculoUuid = veiculoUuid;
            return this;
        }

        public Builder motoristaUuid(String motoristaUuid) {
            this.motoristaUuid = motoristaUuid;
            return this;
        }

        public Builder aderenciaPct(Double aderenciaPct) {
            this.aderenciaPct = aderenciaPct;
            return this;
        }

        public Builder desviosCount(Integer desviosCount) {
            this.desviosCount = desviosCount;
            return this;
        }

        public Builder kmExtrasTotal(Double kmExtrasTotal) {
            this.kmExtrasTotal = kmExtrasTotal;
            return this;
        }

        public Builder duracaoTotalMin(Integer duracaoTotalMin) {
            this.duracaoTotalMin = duracaoTotalMin;
            return this;
        }

        public Builder atrasoChegadaMin(Integer atrasoChegadaMin) {
            this.atrasoChegadaMin = atrasoChegadaMin;
            return this;
        }

        public Builder tempoParadasMin(Integer tempoParadasMin) {
            this.tempoParadasMin = tempoParadasMin;
            return this;
        }

        public Builder tempoOciosoMin(Integer tempoOciosoMin) {
            this.tempoOciosoMin = tempoOciosoMin;
            return this;
        }

        public Builder kmTotal(Double kmTotal) {
            this.kmTotal = kmTotal;
            return this;
        }

        public Builder kmPlanejado(Double kmPlanejado) {
            this.kmPlanejado = kmPlanejado;
            return this;
        }

        public Builder diferencaKm(Double diferencaKm) {
            this.diferencaKm = diferencaKm;
            return this;
        }

        public Builder litrosConsumidos(Double litrosConsumidos) {
            this.litrosConsumidos = litrosConsumidos;
            return this;
        }

        public Builder custoCombustivel(BigDecimal custoCombustivel) {
            this.custoCombustivel = custoCombustivel;
            return this;
        }

        public Builder consumoMedio(Double consumoMedio) {
            this.consumoMedio = consumoMedio;
            return this;
        }

        public Builder scoreFinal(Integer scoreFinal) {
            this.scoreFinal = scoreFinal;
            return this;
        }

        public Builder frenagensBruscas(Integer frenagensBruscas) {
            this.frenagensBruscas = frenagensBruscas;
            return this;
        }

        public Builder excessosVelocidade(Integer excessosVelocidade) {
            this.excessosVelocidade = excessosVelocidade;
            return this;
        }

        public Builder aceleracoesBruscas(Integer aceleracoesBruscas) {
            this.aceleracoesBruscas = aceleracoesBruscas;
            return this;
        }

        public Builder entregasTotal(Integer entregasTotal) {
            this.entregasTotal = entregasTotal;
            return this;
        }

        public Builder entregasSucesso(Integer entregasSucesso) {
            this.entregasSucesso = entregasSucesso;
            return this;
        }

        public Builder entregasFalhas(Integer entregasFalhas) {
            this.entregasFalhas = entregasFalhas;
            return this;
        }

        public Builder entregasNoPrazo(Integer entregasNoPrazo) {
            this.entregasNoPrazo = entregasNoPrazo;
            return this;
        }

        public Builder pdfPath(String pdfPath) {
            this.pdfPath = pdfPath;
            return this;
        }

        public Builder geradoEm(LocalDateTime geradoEm) {
            this.geradoEm = geradoEm;
            return this;
        }

        public Builder viagem(Viagem viagem) {
            this.viagem = viagem;
            return this;
        }

        public RelatorioViagem build() {
            return new RelatorioViagem(
                    id, tenantId, viagemId, rotaId,
                    veiculoUuid, motoristaUuid,
                    aderenciaPct, desviosCount, kmExtrasTotal,
                    duracaoTotalMin, atrasoChegadaMin,
                    tempoParadasMin, tempoOciosoMin,
                    kmTotal, kmPlanejado, diferencaKm,
                    litrosConsumidos, custoCombustivel, consumoMedio,
                    scoreFinal, frenagensBruscas, excessosVelocidade,
                    aceleracoesBruscas, entregasTotal, entregasSucesso,
                    entregasFalhas, entregasNoPrazo, pdfPath,
                    geradoEm, viagem);
        }
    }
}