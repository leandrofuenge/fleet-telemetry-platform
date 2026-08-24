package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * RN-VIA-002 - Score da Viagem (0-1000)
 * Registra o score calculado para cada viagem
 */
@Entity
@Table(name = "score_viagem", indexes = {
    @Index(name = "idx_score_viagem", columnList = "viagem_id"),
    @Index(name = "idx_score_motorista", columnList = "motorista_id"),
    @Index(name = "idx_score_data", columnList = "data_calculo")
})
public class ScoreViagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;

    @Column(name = "motorista_id", nullable = false)
    private Long motoristaId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "score_inicial", nullable = false)
    private Integer scoreInicial = 1000;

    @Column(name = "score_final", nullable = false)
    private Integer scoreFinal = 1000;

    // RN-VIA-002: Penalidades
    @Column(name = "penalidade_frenagem_brusca")
    private Integer penalidadeFrenagemBrusca = 0;

    @Column(name = "penalidade_excesso_velocidade")
    private Integer penalidadeExcessoVelocidade = 0;

    @Column(name = "penalidade_desvio_nao_justificado")
    private Integer penalidadeDesvioNaoJustificado = 0;

    @Column(name = "penalidade_uso_celular")
    private Integer penalidadeUsoCelular = 0;

    @Column(name = "penalidade_entrega_fora_janela")
    private Integer penalidadeEntregaForaJanela = 0;

    @Column(name = "quantidade_frenagem_brusca")
    private Integer quantidadeFrenagemBrusca = 0;

    @Column(name = "quantidade_excesso_velocidade")
    private Integer quantidadeExcessoVelocidade = 0;

    @Column(name = "quantidade_desvio_nao_justificado")
    private Integer quantidadeDesvioNaoJustificado = 0;

    @Column(name = "quantidade_uso_celular")
    private Integer quantidadeUsoCelular = 0;

    @Column(name = "quantidade_entrega_fora_janela")
    private Integer quantidadeEntregaForaJanela = 0;

    @Column(name = "notificacao_gestor_enviada")
    private Boolean notificacaoGestorEnviada = false;

    @Column(name = "score_classificacao", length = 20)
    private String scoreClassificacao; // EXCELENTE, BOM, REGULAR, CRITICO

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @CreationTimestamp
    @Column(name = "data_calculo", nullable = false, updatable = false)
    private LocalDateTime dataCalculo;

    // ================================
    // Construtores
    // ================================

    public ScoreViagem() {
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

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Integer getScoreInicial() {
        return scoreInicial;
    }

    public void setScoreInicial(Integer scoreInicial) {
        this.scoreInicial = scoreInicial;
    }

    public Integer getScoreFinal() {
        return scoreFinal;
    }

    public void setScoreFinal(Integer scoreFinal) {
        this.scoreFinal = scoreFinal;
    }

    public Integer getPenalidadeFrenagemBrusca() {
        return penalidadeFrenagemBrusca;
    }

    public void setPenalidadeFrenagemBrusca(Integer penalidadeFrenagemBrusca) {
        this.penalidadeFrenagemBrusca = penalidadeFrenagemBrusca;
    }

    public Integer getPenalidadeExcessoVelocidade() {
        return penalidadeExcessoVelocidade;
    }

    public void setPenalidadeExcessoVelocidade(Integer penalidadeExcessoVelocidade) {
        this.penalidadeExcessoVelocidade = penalidadeExcessoVelocidade;
    }

    public Integer getPenalidadeDesvioNaoJustificado() {
        return penalidadeDesvioNaoJustificado;
    }

    public void setPenalidadeDesvioNaoJustificado(Integer penalidadeDesvioNaoJustificado) {
        this.penalidadeDesvioNaoJustificado = penalidadeDesvioNaoJustificado;
    }

    public Integer getPenalidadeUsoCelular() {
        return penalidadeUsoCelular;
    }

    public void setPenalidadeUsoCelular(Integer penalidadeUsoCelular) {
        this.penalidadeUsoCelular = penalidadeUsoCelular;
    }

    public Integer getPenalidadeEntregaForaJanela() {
        return penalidadeEntregaForaJanela;
    }

    public void setPenalidadeEntregaForaJanela(Integer penalidadeEntregaForaJanela) {
        this.penalidadeEntregaForaJanela = penalidadeEntregaForaJanela;
    }

    public Integer getQuantidadeFrenagemBrusca() {
        return quantidadeFrenagemBrusca;
    }

    public void setQuantidadeFrenagemBrusca(Integer quantidadeFrenagemBrusca) {
        this.quantidadeFrenagemBrusca = quantidadeFrenagemBrusca;
    }

    public Integer getQuantidadeExcessoVelocidade() {
        return quantidadeExcessoVelocidade;
    }

    public void setQuantidadeExcessoVelocidade(Integer quantidadeExcessoVelocidade) {
        this.quantidadeExcessoVelocidade = quantidadeExcessoVelocidade;
    }

    public Integer getQuantidadeDesvioNaoJustificado() {
        return quantidadeDesvioNaoJustificado;
    }

    public void setQuantidadeDesvioNaoJustificado(Integer quantidadeDesvioNaoJustificado) {
        this.quantidadeDesvioNaoJustificado = quantidadeDesvioNaoJustificado;
    }

    public Integer getQuantidadeUsoCelular() {
        return quantidadeUsoCelular;
    }

    public void setQuantidadeUsoCelular(Integer quantidadeUsoCelular) {
        this.quantidadeUsoCelular = quantidadeUsoCelular;
    }

    public Integer getQuantidadeEntregaForaJanela() {
        return quantidadeEntregaForaJanela;
    }

    public void setQuantidadeEntregaForaJanela(Integer quantidadeEntregaForaJanela) {
        this.quantidadeEntregaForaJanela = quantidadeEntregaForaJanela;
    }

    public Boolean getNotificacaoGestorEnviada() {
        return notificacaoGestorEnviada;
    }

    public void setNotificacaoGestorEnviada(Boolean notificacaoGestorEnviada) {
        this.notificacaoGestorEnviada = notificacaoGestorEnviada;
    }

    public String getScoreClassificacao() {
        return scoreClassificacao;
    }

    public void setScoreClassificacao(String scoreClassificacao) {
        this.scoreClassificacao = scoreClassificacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataCalculo() {
        return dataCalculo;
    }

    // ================================
    // Métodos utilitários
    // ================================

    /**
     * Calcula a classificação baseada no score final
     */
    public void calcularClassificacao() {
        if (scoreFinal == null) {
            scoreClassificacao = "INDEFINIDO";
        } else if (scoreFinal >= 900) {
            scoreClassificacao = "EXCELENTE";
        } else if (scoreFinal >= 700) {
            scoreClassificacao = "BOM";
        } else if (scoreFinal >= 500) {
            scoreClassificacao = "REGULAR";
        } else {
            scoreClassificacao = "CRITICO";
        }
    }

    /**
     * Verifica se o score é crítico (< 700)
     */
    public boolean isScoreCritico() {
        return scoreFinal != null && scoreFinal < 700;
    }

    /**
     * Calcula o score final baseado nas penalidades
     */
    public void calcularScoreFinal() {
        int penalidadeTotal = (penalidadeFrenagemBrusca != null ? penalidadeFrenagemBrusca : 0)
                + (penalidadeExcessoVelocidade != null ? penalidadeExcessoVelocidade : 0)
                + (penalidadeDesvioNaoJustificado != null ? penalidadeDesvioNaoJustificado : 0)
                + (penalidadeUsoCelular != null ? penalidadeUsoCelular : 0)
                + (penalidadeEntregaForaJanela != null ? penalidadeEntregaForaJanela : 0);

        this.scoreFinal = Math.max(0, this.scoreInicial - penalidadeTotal);
        calcularClassificacao();
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
        private Long motoristaId;
        private Long veiculoId;
        private Integer scoreInicial = 1000;
        private Integer scoreFinal = 1000;
        private Integer penalidadeFrenagemBrusca = 0;
        private Integer penalidadeExcessoVelocidade = 0;
        private Integer penalidadeDesvioNaoJustificado = 0;
        private Integer penalidadeUsoCelular = 0;
        private Integer penalidadeEntregaForaJanela = 0;
        private Integer quantidadeFrenagemBrusca = 0;
        private Integer quantidadeExcessoVelocidade = 0;
        private Integer quantidadeDesvioNaoJustificado = 0;
        private Integer quantidadeUsoCelular = 0;
        private Integer quantidadeEntregaForaJanela = 0;
        private Boolean notificacaoGestorEnviada = false;
        private String observacoes;

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

        public Builder motoristaId(Long motoristaId) {
            this.motoristaId = motoristaId;
            return this;
        }

        public Builder veiculoId(Long veiculoId) {
            this.veiculoId = veiculoId;
            return this;
        }

        public Builder scoreInicial(Integer scoreInicial) {
            this.scoreInicial = scoreInicial;
            return this;
        }

        public Builder penalidadeFrenagemBrusca(Integer penalidadeFrenagemBrusca) {
            this.penalidadeFrenagemBrusca = penalidadeFrenagemBrusca;
            return this;
        }

        public Builder penalidadeExcessoVelocidade(Integer penalidadeExcessoVelocidade) {
            this.penalidadeExcessoVelocidade = penalidadeExcessoVelocidade;
            return this;
        }

        public Builder penalidadeDesvioNaoJustificado(Integer penalidadeDesvioNaoJustificado) {
            this.penalidadeDesvioNaoJustificado = penalidadeDesvioNaoJustificado;
            return this;
        }

        public Builder penalidadeUsoCelular(Integer penalidadeUsoCelular) {
            this.penalidadeUsoCelular = penalidadeUsoCelular;
            return this;
        }

        public Builder penalidadeEntregaForaJanela(Integer penalidadeEntregaForaJanela) {
            this.penalidadeEntregaForaJanela = penalidadeEntregaForaJanela;
            return this;
        }

        public Builder quantidadeFrenagemBrusca(Integer quantidadeFrenagemBrusca) {
            this.quantidadeFrenagemBrusca = quantidadeFrenagemBrusca;
            return this;
        }

        public Builder quantidadeExcessoVelocidade(Integer quantidadeExcessoVelocidade) {
            this.quantidadeExcessoVelocidade = quantidadeExcessoVelocidade;
            return this;
        }

        public Builder quantidadeDesvioNaoJustificado(Integer quantidadeDesvioNaoJustificado) {
            this.quantidadeDesvioNaoJustificado = quantidadeDesvioNaoJustificado;
            return this;
        }

        public Builder quantidadeUsoCelular(Integer quantidadeUsoCelular) {
            this.quantidadeUsoCelular = quantidadeUsoCelular;
            return this;
        }

        public Builder quantidadeEntregaForaJanela(Integer quantidadeEntregaForaJanela) {
            this.quantidadeEntregaForaJanela = quantidadeEntregaForaJanela;
            return this;
        }

        public Builder notificacaoGestorEnviada(Boolean notificacaoGestorEnviada) {
            this.notificacaoGestorEnviada = notificacaoGestorEnviada;
            return this;
        }

        public Builder observacoes(String observacoes) {
            this.observacoes = observacoes;
            return this;
        }

        public ScoreViagem build() {
            ScoreViagem score = new ScoreViagem();
            score.setId(this.id);
            score.setTenantId(this.tenantId);
            score.setViagemId(this.viagemId);
            score.setMotoristaId(this.motoristaId);
            score.setVeiculoId(this.veiculoId);
            score.setScoreInicial(this.scoreInicial);
            score.setPenalidadeFrenagemBrusca(this.penalidadeFrenagemBrusca);
            score.setPenalidadeExcessoVelocidade(this.penalidadeExcessoVelocidade);
            score.setPenalidadeDesvioNaoJustificado(this.penalidadeDesvioNaoJustificado);
            score.setPenalidadeUsoCelular(this.penalidadeUsoCelular);
            score.setPenalidadeEntregaForaJanela(this.penalidadeEntregaForaJanela);
            score.setQuantidadeFrenagemBrusca(this.quantidadeFrenagemBrusca);
            score.setQuantidadeExcessoVelocidade(this.quantidadeExcessoVelocidade);
            score.setQuantidadeDesvioNaoJustificado(this.quantidadeDesvioNaoJustificado);
            score.setQuantidadeUsoCelular(this.quantidadeUsoCelular);
            score.setQuantidadeEntregaForaJanela(this.quantidadeEntregaForaJanela);
            score.setNotificacaoGestorEnviada(this.notificacaoGestorEnviada);
            score.setObservacoes(this.observacoes);
            score.calcularScoreFinal();
            return score;
        }
    }

    @Override
    public String toString() {
        return "ScoreViagem{" +
                "id=" + id +
                ", viagemId=" + viagemId +
                ", motoristaId=" + motoristaId +
                ", scoreFinal=" + scoreFinal +
                ", classificacao='" + scoreClassificacao + '\'' +
                '}';
    }
}