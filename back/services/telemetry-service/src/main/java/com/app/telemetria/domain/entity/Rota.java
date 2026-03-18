package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.telemetria.util.PontoRotaConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "rotas", indexes = {
        @Index(name = "idx_rot_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_rot_motorista", columnList = "motorista_id"),
        @Index(name = "idx_rot_status", columnList = "status"),
        @Index(name = "idx_rot_ativa", columnList = "ativa")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // ── Origem ────────────────────────────────────────────────
    @Column(length = 255)
    private String origem;

    @Column(name = "latitude_origem")
    private Double latitudeOrigem;

    @Column(name = "longitude_origem")
    private Double longitudeOrigem;

    // ── Destino ───────────────────────────────────────────────
    @Column(length = 255)
    private String destino;

    @Column(name = "latitude_destino")
    private Double latitudeDestino;

    @Column(name = "longitude_destino")
    private Double longitudeDestino;

    // ── Planejamento ──────────────────────────────────────────
    @Column(name = "distancia_prevista")
    private Double distanciaPrevista;

    @Column(name = "tempo_previsto")
    private Integer tempoPrevisto; // minutos

    // ── Thresholds de desvio ──────────────────────────────────
    @Column(name = "tolerancia_desvio_m")
    private Double toleranciaDesvioM = 100.0;

    @Column(name = "threshold_alerta_m")
    private Double thresholdAlertaM = 50.0;

    // ── Controle ──────────────────────────────────────────────
    @Column(nullable = false)
    private String status = "PLANEJADA"; // PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA

    @Column(name = "ativa", nullable = false)
    private Boolean ativa = true;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    // ── Pontos intermediários (JSON) ───────────────────────────
    @Convert(converter = PontoRotaConverter.class)
    @Column(name = "pontos_rota", columnDefinition = "JSON")
    private List<PontoRota> pontosRota = new ArrayList<>();

    // ── Relacionamentos ────────────────────────────────────────

    /**
     * Veículo designado para esta rota.
     * Lado N do N:1 com Veiculo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    @JsonIgnore
    private Veiculo veiculo;

    /**
     * Motorista responsável pela rota.
     * Lado N do N:1 com Motorista.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    @JsonIgnore
    private Motorista motorista;

    /**
     * Viagens que executaram esta rota.
     * Uma rota pode ser executada várias vezes (rota recorrente).
     * Mapeado pelo campo "rota" em Viagem.
     */
    @OneToMany(mappedBy = "rota", fetch = FetchType.LAZY, cascade = {})
    @JsonIgnore
    private List<Viagem> viagens = new ArrayList<>();

    /**
     * Desvios detectados em relação a esta rota.
     * Mapeado pelo campo "rota" em DesvioRota.
     */
    @OneToMany(mappedBy = "rota", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DesvioRota> desvios = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Construtores ───────────────────────────────────────────

    public Rota() {
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public Double getLatitudeOrigem() {
        return latitudeOrigem;
    }

    public void setLatitudeOrigem(Double latitudeOrigem) {
        this.latitudeOrigem = latitudeOrigem;
    }

    public Double getLongitudeOrigem() {
        return longitudeOrigem;
    }

    public void setLongitudeOrigem(Double longitudeOrigem) {
        this.longitudeOrigem = longitudeOrigem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Double getLatitudeDestino() {
        return latitudeDestino;
    }

    public void setLatitudeDestino(Double latitudeDestino) {
        this.latitudeDestino = latitudeDestino;
    }

    public Double getLongitudeDestino() {
        return longitudeDestino;
    }

    public void setLongitudeDestino(Double longitudeDestino) {
        this.longitudeDestino = longitudeDestino;
    }

    public Double getDistanciaPrevista() {
        return distanciaPrevista;
    }

    public void setDistanciaPrevista(Double distanciaPrevista) {
        this.distanciaPrevista = distanciaPrevista;
    }

    public Integer getTempoPrevisto() {
        return tempoPrevisto;
    }

    public void setTempoPrevisto(Integer tempoPrevisto) {
        this.tempoPrevisto = tempoPrevisto;
    }

    public Double getToleranciaDesvioM() {
        return toleranciaDesvioM;
    }

    public void setToleranciaDesvioM(Double toleranciaDesvioM) {
        this.toleranciaDesvioM = toleranciaDesvioM;
    }

    public Double getThresholdAlertaM() {
        return thresholdAlertaM;
    }

    public void setThresholdAlertaM(Double thresholdAlertaM) {
        this.thresholdAlertaM = thresholdAlertaM;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public List<PontoRota> getPontosRota() {
        return pontosRota;
    }

    public void setPontosRota(List<PontoRota> pontosRota) {
        this.pontosRota = pontosRota;
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

    public List<Viagem> getViagens() {
        return viagens;
    }

    public void setViagens(List<Viagem> viagens) {
        this.viagens = viagens;
    }

    public List<DesvioRota> getDesvios() {
        return desvios;
    }

    public void setDesvios(List<DesvioRota> desvios) {
        this.desvios = desvios;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
