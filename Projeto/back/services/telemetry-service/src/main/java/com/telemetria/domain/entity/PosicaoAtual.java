package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

// ─────────────────────────────────────────────────────────────
// 8. PosicaoAtual.java
// ─────────────────────────────────────────────────────────────
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posicao_atual", indexes = {
        @Index(name = "idx_pa_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pa_status", columnList = "status_veiculo")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosicaoAtual {

    @Id
    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "velocidade", nullable = false)
    @Builder.Default
    private Double velocidade = 0.0;

    @Column(name = "direcao")
    private Double direcao;

    @Column(name = "ignicao", nullable = false)
    @Builder.Default
    private Boolean ignicao = false;

    @Column(name = "status_veiculo", nullable = false, length = 30)
    @Builder.Default
    private String statusVeiculo = "DESCONHECIDO";

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "odometro")
    private Double odometro;

    @Column(name = "nivel_combustivel")
    private Double nivelCombustivel;

    @Column(name = "bateria_v")
    private Double bateriaV;

    @Column(name = "ultima_telemetria", nullable = false)
    private LocalDateTime ultimaTelemetria;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @Column(name = "nome_local")
    private String nomeLocal;

    @Column(name = "alertas_ativos", nullable = false)
    @Builder.Default
    private Integer alertasAtivos = 0;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private VeiculoCache veiculo;

    // ==================== GETTERS ====================

    public Long getVeiculoId() {
        return veiculoId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getVeiculoUuid() {
        return veiculoUuid;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getVelocidade() {
        return velocidade;
    }

    public Double getDirecao() {
        return direcao;
    }

    public Boolean getIgnicao() {
        return ignicao;
    }

    public String getStatusVeiculo() {
        return statusVeiculo;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public Long getViagemId() {
        return viagemId;
    }

    public Double getOdometro() {
        return odometro;
    }

    public Double getNivelCombustivel() {
        return nivelCombustivel;
    }

    public Double getBateriaV() {
        return bateriaV;
    }

    public LocalDateTime getUltimaTelemetria() {
        return ultimaTelemetria;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public String getNomeLocal() {
        return nomeLocal;
    }

    public Integer getAlertasAtivos() {
        return alertasAtivos;
    }

    public VeiculoCache getVeiculo() {
        return veiculo;
    }

    // ==================== SETTERS ====================

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setVeiculoUuid(String veiculoUuid) {
        this.veiculoUuid = veiculoUuid;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setVelocidade(Double velocidade) {
        this.velocidade = velocidade;
    }

    public void setDirecao(Double direcao) {
        this.direcao = direcao;
    }

    public void setIgnicao(Boolean ignicao) {
        this.ignicao = ignicao;
    }

    public void setStatusVeiculo(String statusVeiculo) {
        this.statusVeiculo = statusVeiculo;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }

    public void setOdometro(Double odometro) {
        this.odometro = odometro;
    }

    public void setNivelCombustivel(Double nivelCombustivel) {
        this.nivelCombustivel = nivelCombustivel;
    }

    public void setBateriaV(Double bateriaV) {
        this.bateriaV = bateriaV;
    }

    public void setUltimaTelemetria(LocalDateTime ultimaTelemetria) {
        this.ultimaTelemetria = ultimaTelemetria;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public void setNomeLocal(String nomeLocal) {
        this.nomeLocal = nomeLocal;
    }

    public void setAlertasAtivos(Integer alertasAtivos) {
        this.alertasAtivos = alertasAtivos;
    }

    public void setVeiculo(VeiculoCache veiculo) {
        this.veiculo = veiculo;
    }
}
