package com.app.telemetria.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "veiculos", indexes = {
        @Index(name = "idx_vei_placa", columnList = "placa"),
        @Index(name = "idx_vei_ativo", columnList = "ativo")
})
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Placa é obrigatória")
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(length = 255)
    private String modelo;

    @Column(length = 100)
    private String marca;

    @Column(name = "capacidade_carga")
    private Double capacidadeCarga;

    @Column(name = "ano_fabricacao")
    private Integer anoFabricacao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // ── Relacionamento com Cliente (proprietário da frota) ─────

    /**
     * Muitos veículos pertencem a um cliente.
     * Lado N do relacionamento N:1 com Cliente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // ── Relacionamento com Motorista atual ─────────────────────

    /**
     * Motorista atualmente vinculado ao veículo (opcional).
     * Não é o histórico — para isso use Viagem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_atual_id")
    private Motorista motoristaAtual;

    // ── Relacionamentos de histórico (evitar N+1) ──────────────

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY, cascade = {})
    private List<Telemetria> historicoTelemetria = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY, cascade = {})
    private List<Rota> rotas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY, cascade = {})
    private List<Viagem> viagens = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Manutencao> manutencoes = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Construtores ───────────────────────────────────────────

    public Veiculo() {
    }

    public Veiculo(String placa, String modelo, Double capacidadeCarga) {
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadeCarga = capacidadeCarga;
        this.ativo = true;
    }

    // ── Métodos utilitários ────────────────────────────────────

    public void addRota(Rota rota) {
        rotas.add(rota);
        rota.setVeiculo(this);
    }

    public void removeRota(Rota rota) {
        rotas.remove(rota);
        rota.setVeiculo(null);
    }

    public void addTelemetria(Telemetria telemetria) {
        historicoTelemetria.add(telemetria);
        telemetria.setVeiculo(this);
    }

    public void addManutencao(Manutencao manutencao) {
        manutencoes.add(manutencao);
        manutencao.setVeiculo(this);
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Double getCapacidadeCarga() {
        return capacidadeCarga;
    }

    public void setCapacidadeCarga(Double capacidadeCarga) {
        this.capacidadeCarga = capacidadeCarga;
    }

    public Integer getAnoFabricacao() {
        return anoFabricacao;
    }

    public void setAnoFabricacao(Integer anoFabricacao) {
        this.anoFabricacao = anoFabricacao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Motorista getMotoristaAtual() {
        return motoristaAtual;
    }

    public void setMotoristaAtual(Motorista motoristaAtual) {
        this.motoristaAtual = motoristaAtual;
    }

    public List<Telemetria> getHistoricoTelemetria() {
        return historicoTelemetria;
    }

    public void setHistoricoTelemetria(List<Telemetria> historicoTelemetria) {
        this.historicoTelemetria = historicoTelemetria;
    }

    public List<Rota> getRotas() {
        return rotas;
    }

    public void setRotas(List<Rota> rotas) {
        this.rotas = rotas;
    }

    public List<Viagem> getViagens() {
        return viagens;
    }

    public void setViagens(List<Viagem> viagens) {
        this.viagens = viagens;
    }

    public List<Manutencao> getManutencoes() {
        return manutencoes;
    }

    public void setManutencoes(List<Manutencao> manutencoes) {
        this.manutencoes = manutencoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Veiculo{id=" + id + ", placa='" + placa + "', modelo='" + modelo + "'}";
    }
}
