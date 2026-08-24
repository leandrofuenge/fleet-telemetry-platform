package com.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "manutencoes", indexes = {
        @Index(name = "idx_man_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_man_data", columnList = "data_manutencao"),
        @Index(name = "idx_man_tipo", columnList = "tipo")
})
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_manutencao", nullable = false)
    private LocalDate dataManutencao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "custo")
    private Double custo;
    @Column(name = "custo_pecas") private Double custoPecas = 0.0;
    @Column(name = "custo_mao_obra") private Double custoMaoObra = 0.0;
    @Column(name = "nota_fiscal_path", length = 500) private String notaFiscalPath;
    @Column(name = "data_agendada") private LocalDate dataAgendada;
    @Column(name = "status", nullable = false, length = 20) private String status = "AGENDADA";
    @Column(name = "anomaly_score") private Double anomalyScore;
    @Column(name = "rul_dias_estimado") private Integer rulDiasEstimado;
    @Column(name = "probabilidade_falha") private Double probabilidadeFalha;

    @Column(nullable = false, length = 30)
    private String tipo; // PREVENTIVA, CORRETIVA, PREDITIVA

    @Column(length = 100)
    private String oficina;

    @Column(name = "km_realizacao")
    private Double kmRealizacao;

    @Column(name = "proxima_manutencao_km")
    private Double proximaManutencaoKm;

    @Column(name = "proxima_manutencao_data")
    private LocalDate proximaManutencaoData;

    @Column(length = 500)
    private String observacoes;

    // ── Relacionamento principal: Manutenção pertence a um Veículo ──

    /**
     * Muitas manutenções pertencem a um veículo.
     * Lado N do N:1 com Veiculo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    /**
     * Motorista que reportou ou estava no veículo (opcional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Construtores ───────────────────────────────────────────

    public Manutencao() {
    }

    public Manutencao(Veiculo veiculo, LocalDate dataManutencao, String descricao, String tipo) {
        this.veiculo = veiculo;
        this.dataManutencao = dataManutencao;
        this.descricao = descricao;
        this.tipo = tipo;
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDataManutencao() {
        return dataManutencao;
    }

    public void setDataManutencao(LocalDate dataManutencao) {
        this.dataManutencao = dataManutencao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getCusto() {
        return custo;
    }
    public Double getCustoPecas(){return custoPecas;} public void setCustoPecas(Double v){custoPecas=v;}
    public Double getCustoMaoObra(){return custoMaoObra;} public void setCustoMaoObra(Double v){custoMaoObra=v;}
    public String getNotaFiscalPath(){return notaFiscalPath;} public void setNotaFiscalPath(String v){notaFiscalPath=v;}
    public LocalDate getDataAgendada(){return dataAgendada;} public void setDataAgendada(LocalDate v){dataAgendada=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Double getAnomalyScore(){return anomalyScore;} public void setAnomalyScore(Double v){anomalyScore=v;}
    public Integer getRulDiasEstimado(){return rulDiasEstimado;} public void setRulDiasEstimado(Integer v){rulDiasEstimado=v;}
    public Double getProbabilidadeFalha(){return probabilidadeFalha;} public void setProbabilidadeFalha(Double v){probabilidadeFalha=v;}

    public void setCusto(Double custo) {
        this.custo = custo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getOficina() {
        return oficina;
    }

    public void setOficina(String oficina) {
        this.oficina = oficina;
    }

    public Double getKmRealizacao() {
        return kmRealizacao;
    }

    public void setKmRealizacao(Double kmRealizacao) {
        this.kmRealizacao = kmRealizacao;
    }

    public Double getProximaManutencaoKm() {
        return proximaManutencaoKm;
    }

    public void setProximaManutencaoKm(Double proximaManutencaoKm) {
        this.proximaManutencaoKm = proximaManutencaoKm;
    }

    public LocalDate getProximaManutencaoData() {
        return proximaManutencaoData;
    }

    public void setProximaManutencaoData(LocalDate proximaManutencaoData) {
        this.proximaManutencaoData = proximaManutencaoData;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
