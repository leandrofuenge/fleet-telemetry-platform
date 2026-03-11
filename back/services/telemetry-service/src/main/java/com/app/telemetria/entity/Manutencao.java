package com.app.telemetria.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
