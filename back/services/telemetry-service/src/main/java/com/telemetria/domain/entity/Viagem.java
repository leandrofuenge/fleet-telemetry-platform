package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

@Entity
@Table(name = "viagens", indexes = {
        @Index(name = "idx_via_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_via_motorista", columnList = "motorista_id"),
        @Index(name = "idx_via_rota", columnList = "rota_id"),
        @Index(name = "idx_via_status", columnList = "status"),
        @Index(name = "idx_via_saida", columnList = "data_saida")
})
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Status e Observações ───────────────────────────────────

    @Column(nullable = false, length = 20)
    private String status = "PLANEJADA"; // PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // ── Datas ─────────────────────────────────────────────────

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "data_chegada_prevista")
    private LocalDateTime dataChegadaPrevista;

    @Column(name = "data_chegada_real")
    private LocalDateTime dataChegadaReal;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    // ── Métricas reais ─────────────────────────────────────────

    @Column(name = "distancia_real_km")
    private Double distanciaRealKm = 0.0;

    @Column(name = "km_fora_rota")
    private Double kmForaRota = 0.0;

    @Column(name = "score_viagem")
    private Integer scoreViagem = 1000;

    // ── IDs das FKs (read-only) ────────────────────────────────

    /**
     * ID do veículo (apenas leitura, espelha a FK)
     */
    @Column(name = "veiculo_id", insertable = false, updatable = false)
    private Long veiculoId;

    /**
     * ID do motorista (apenas leitura, espelha a FK)
     */
    @Column(name = "motorista_id", insertable = false, updatable = false)
    private Long motoristaId;

    // ── Relacionamentos principais ─────────────────────────────

    /**
     * Veículo que realizou a viagem.
     * Lado N do N:1 com Veiculo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    /**
     * Motorista responsável pela viagem.
     * Lado N do N:1 com Motorista.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;

    /**
     * Carga transportada nesta viagem.
     * Lado N do N:1 com Carga.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id")
    private Carga carga;

    /**
     * Rota planejada que esta viagem está executando.
     * Lado N do N:1 com Rota.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id")
    private Rota rota;

    // ── Relacionamentos derivados ──────────────────────────────

    /**
     * Registros de telemetria gerados durante esta viagem.
     * Mapeado pelo campo "viagem" em Telemetria (via viagemId).
     * NÃO usar cascade — telemetria é imutável após gravação.
     */
    @OneToMany(mappedBy = "viagem", fetch = FetchType.LAZY, cascade = {})
    private List<Telemetria> telemetrias = new ArrayList<>();

    /**
     * Alertas gerados durante esta viagem.
     * Mapeado pelo campo "viagem" em Alerta.
     */
    @OneToMany(mappedBy = "viagem", fetch = FetchType.LAZY, cascade = {})
    private List<Alerta> alertas = new ArrayList<>();

    /**
     * Desvios de rota detectados nesta viagem.
     * Mapeado pelo campo "viagem" em DesvioRota.
     */
    @OneToMany(mappedBy = "viagem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DesvioRota> desvios = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Construtores ───────────────────────────────────────────

    public Viagem() {
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
    }

    public LocalDateTime getDataChegadaPrevista() {
        return dataChegadaPrevista;
    }

    public void setDataChegadaPrevista(LocalDateTime dataChegadaPrevista) {
        this.dataChegadaPrevista = dataChegadaPrevista;
    }

    public LocalDateTime getDataChegadaReal() {
        return dataChegadaReal;
    }

    public void setDataChegadaReal(LocalDateTime dataChegadaReal) {
        this.dataChegadaReal = dataChegadaReal;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Double getDistanciaRealKm() {
        return distanciaRealKm;
    }

    public void setDistanciaRealKm(Double distanciaRealKm) {
        this.distanciaRealKm = distanciaRealKm;
    }

    public Double getKmForaRota() {
        return kmForaRota;
    }

    public void setKmForaRota(Double kmForaRota) {
        this.kmForaRota = kmForaRota;
    }

    public Integer getScoreViagem() {
        return scoreViagem;
    }

    public void setScoreViagem(Integer scoreViagem) {
        this.scoreViagem = scoreViagem;
    }

    // ── Getters e setters para IDs read-only ───────────────────

    public Long getVeiculoId() {
        if (veiculoId == null && veiculo != null) {
            veiculoId = veiculo.getId();
        }
        return veiculoId;
    }

    public Long getMotoristaId() {
        if (motoristaId == null && motorista != null) {
            motoristaId = motorista.getId();
        }
        return motoristaId;
    }

    // ── Getters e setters para relacionamentos principais ──────

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
        if (veiculo != null) {
            this.veiculoId = veiculo.getId();
        } else {
            this.veiculoId = null;
        }
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
        if (motorista != null) {
            this.motoristaId = motorista.getId();
        } else {
            this.motoristaId = null;
        }
    }

    public Carga getCarga() {
        return carga;
    }

    public void setCarga(Carga carga) {
        this.carga = carga;
    }

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }

    // ── Getters e setters para listas ──────────────────────────

    public List<Telemetria> getTelemetrias() {
        return telemetrias;
    }

    public void setTelemetrias(List<Telemetria> telemetrias) {
        this.telemetrias = telemetrias;
    }

    public List<Alerta> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<Alerta> alertas) {
        this.alertas = alertas;
    }

    public List<DesvioRota> getDesvios() {
        return desvios;
    }

    public void setDesvios(List<DesvioRota> desvios) {
        this.desvios = desvios;
    }

    // ── Auditoria ──────────────────────────────────────────────

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
