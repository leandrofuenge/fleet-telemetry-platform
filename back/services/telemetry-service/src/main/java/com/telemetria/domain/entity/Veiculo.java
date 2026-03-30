package com.telemetria.domain.entity;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "veiculos", 
       indexes = {
           @Index(name = "idx_vei_placa", columnList = "placa"),
           @Index(name = "idx_vei_tenant", columnList = "tenant_id"),
           @Index(name = "idx_vei_ativo", columnList = "ativo")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_veiculo_placa_tenant", 
                             columnNames = {"placa", "tenant_id"})
       })
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== NOVO: Tenant ID =====
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // RN-VEI-001: Formato Mercosul ou antigo
    @NotBlank(message = "Placa é obrigatória")
    @Pattern(regexp = "^([A-Z]{3}\\d{1}[A-Z]{1}\\d{2}|[A-Z]{3}-\\d{4})$", 
             message = "Formato inválido. Use ABC1D23 (Mercosul) ou ABC-1234")
    @Column(nullable = false, length = 10)
    private String placa;

    @Column(length = 255)
    private String modelo;

    @Column(length = 100)
    private String marca;
    
    @Column(name = "plano", length = 20)
    private String plano = "STARTER"; // STARTER, PRO, ENTERPRISE

    @Column(name = "capacidade_carga")
    private Double capacidadeCarga;

    @Column(name = "ano_fabricacao")
    private Integer anoFabricacao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // ===== RN-VEI-002: Tacógrafo =====
    @Column(name = "pbt_kg")
    private Double pbt; // Peso Bruto Total em kg

    @Column(name = "tacografo_obrigatorio")
    private Boolean tacografoObrigatorio = false;

    @Column(name = "data_venc_tacografo")
    private LocalDate dataVencimentoTacografo;

    // ===== RN-VEI-003: Documentos com Vencimento =====
    @Column(name = "data_venc_crlv")
    private LocalDate dataVencimentoCrlv;

    @Column(name = "data_venc_seguro")
    private LocalDate dataVencimentoSeguro;

    @Column(name = "data_venc_dpvat")
    private LocalDate dataVencimentoDpvat;

    @Column(name = "data_venc_rcf")
    private LocalDate dataVencimentoRcf;

    @Column(name = "data_venc_vistoria")
    private LocalDate dataVencimentoVistoria;

    @Column(name = "data_venc_rntrc")
    private LocalDate dataVencimentoRntrc;

    // ── IDs para consultas diretas ─────
    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Long clienteId;

    @Column(name = "motorista_atual_id", insertable = false, updatable = false)
    private Long motoristaAtualId;

    // ── Relacionamentos JPA ─────
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

    // ===== Método utilitário para tacógrafo =====
    public void atualizarTacografoObrigatorio() {
        if (pbt != null && pbt > 4536.0) {
            this.tacografoObrigatorio = true;
        }
    }

    // ===== Getters e Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

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

    // Tacógrafo
    public Double getPbt() { return pbt; }
    public void setPbt(Double pbt) { 
        this.pbt = pbt;
        atualizarTacografoObrigatorio();
    }

    public Boolean getTacografoObrigatorio() { return tacografoObrigatorio; }
    public void setTacografoObrigatorio(Boolean tacografoObrigatorio) { this.tacografoObrigatorio = tacografoObrigatorio; }

    public LocalDate getDataVencimentoTacografo() { return dataVencimentoTacografo; }
    public void setDataVencimentoTacografo(LocalDate dataVencimentoTacografo) { this.dataVencimentoTacografo = dataVencimentoTacografo; }

    // Documentos
    public LocalDate getDataVencimentoCrlv() { return dataVencimentoCrlv; }
    public void setDataVencimentoCrlv(LocalDate dataVencimentoCrlv) { this.dataVencimentoCrlv = dataVencimentoCrlv; }

    public LocalDate getDataVencimentoSeguro() { return dataVencimentoSeguro; }
    public void setDataVencimentoSeguro(LocalDate dataVencimentoSeguro) { this.dataVencimentoSeguro = dataVencimentoSeguro; }

    public LocalDate getDataVencimentoDpvat() { return dataVencimentoDpvat; }
    public void setDataVencimentoDpvat(LocalDate dataVencimentoDpvat) { this.dataVencimentoDpvat = dataVencimentoDpvat; }

    public LocalDate getDataVencimentoRcf() { return dataVencimentoRcf; }
    public void setDataVencimentoRcf(LocalDate dataVencimentoRcf) { this.dataVencimentoRcf = dataVencimentoRcf; }

    public LocalDate getDataVencimentoVistoria() { return dataVencimentoVistoria; }
    public void setDataVencimentoVistoria(LocalDate dataVencimentoVistoria) { this.dataVencimentoVistoria = dataVencimentoVistoria; }

    public LocalDate getDataVencimentoRntrc() { return dataVencimentoRntrc; }
    public void setDataVencimentoRntrc(LocalDate dataVencimentoRntrc) { this.dataVencimentoRntrc = dataVencimentoRntrc; }

    // IDs
    public Long getClienteId() { return cliente != null ? cliente.getId() : clienteId; }
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
        if (this.cliente != null && !this.cliente.getId().equals(clienteId)) {
            this.cliente = null;
        }
    }

    public Long getMotoristaAtualId() { return motoristaAtual != null ? motoristaAtual.getId() : motoristaAtualId; }
    public void setMotoristaAtualId(Long motoristaAtualId) {
        this.motoristaAtualId = motoristaAtualId;
        if (this.motoristaAtual != null && !this.motoristaAtual.getId().equals(motoristaAtualId)) {
            this.motoristaAtual = null;
        }
    }

    // Relacionamentos
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { 
        this.cliente = cliente;
        this.clienteId = cliente != null ? cliente.getId() : null;
        if (cliente != null) {
            this.tenantId = cliente.getId(); // tenantId = cliente.id
        }
    }

    public Motorista getMotoristaAtual() { return motoristaAtual; }
    public void setMotoristaAtual(Motorista motoristaAtual) { 
        this.motoristaAtual = motoristaAtual;
        this.motoristaAtualId = motoristaAtual != null ? motoristaAtual.getId() : null;
    }

    // Coleções
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
    
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    
    
}