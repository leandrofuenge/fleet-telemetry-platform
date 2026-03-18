package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

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

    // ── IDs para consultas diretas (usados no VeiculoService) ─────
    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Long clienteId;

    @Column(name = "motorista_atual_id", insertable = false, updatable = false)
    private Long motoristaAtualId;

    // ── Relacionamentos JPA completos ─────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_atual_id")
    private Motorista motoristaAtual;

    // ── Coleções ─────
    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY)
    private List<Telemetria> historicoTelemetria = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY)
    private List<Rota> rotas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY)
    private List<Viagem> viagens = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Manutencao> manutencoes = new ArrayList<>();

    // ── Auditoria ─────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ===== Construtores =====
    public Veiculo() {}

    public Veiculo(String placa, String modelo, Double capacidadeCarga) {
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadeCarga = capacidadeCarga;
        this.ativo = true;
    }

    // ===== Métodos utilitários =====
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

    // ===== Getters e Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Double getCapacidadeCarga() { return capacidadeCarga; }
    public void setCapacidadeCarga(Double capacidadeCarga) { this.capacidadeCarga = capacidadeCarga; }

    public Integer getAnoFabricacao() { return anoFabricacao; }
    public void setAnoFabricacao(Integer anoFabricacao) { this.anoFabricacao = anoFabricacao; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    // Getters para os IDs
    public Long getClienteId() { 
        return cliente != null ? cliente.getId() : clienteId; 
    }

    public Long getMotoristaAtualId() { 
        return motoristaAtual != null ? motoristaAtual.getId() : motoristaAtualId; 
    }

    // ===== NOVOS MÉTODOS ADICIONADOS =====
    
    /**
     * Define o ID do cliente diretamente
     * Útil para operações que usam apenas o ID
     */
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
        // Se tivermos o cliente carregado, atualizamos também
        if (this.cliente != null && !this.cliente.getId().equals(clienteId)) {
            this.cliente = null; // Invalida o objeto cliente se o ID for diferente
        }
    }

    /**
     * Define o ID do motorista atual diretamente
     * Útil para operações que usam apenas o ID
     */
    public void setMotoristaAtualId(Long motoristaAtualId) {
        this.motoristaAtualId = motoristaAtualId;
        // Se tivermos o motorista carregado, atualizamos também
        if (this.motoristaAtual != null && !this.motoristaAtual.getId().equals(motoristaAtualId)) {
            this.motoristaAtual = null; // Invalida o objeto motorista se o ID for diferente
        }
    }

    // Getters e Setters dos relacionamentos
    public Cliente getCliente() { return cliente; }
    
    public void setCliente(Cliente cliente) { 
        this.cliente = cliente;
        this.clienteId = cliente != null ? cliente.getId() : null;
    }

    public Motorista getMotoristaAtual() { return motoristaAtual; }
    
    public void setMotoristaAtual(Motorista motoristaAtual) { 
        this.motoristaAtual = motoristaAtual;
        this.motoristaAtualId = motoristaAtual != null ? motoristaAtual.getId() : null;
    }

    // Getters e Setters das coleções
    public List<Telemetria> getHistoricoTelemetria() { return historicoTelemetria; }
    public void setHistoricoTelemetria(List<Telemetria> historicoTelemetria) { this.historicoTelemetria = historicoTelemetria; }

    public List<Rota> getRotas() { return rotas; }
    public void setRotas(List<Rota> rotas) { this.rotas = rotas; }

    public List<Viagem> getViagens() { return viagens; }
    public void setViagens(List<Viagem> viagens) { this.viagens = viagens; }

    public List<Manutencao> getManutencoes() { return manutencoes; }
    public void setManutencoes(List<Manutencao> manutencoes) { this.manutencoes = manutencoes; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public String toString() {
        return "Veiculo{id=" + id + ", placa='" + placa + "', modelo='" + modelo + "'}";
    }
}