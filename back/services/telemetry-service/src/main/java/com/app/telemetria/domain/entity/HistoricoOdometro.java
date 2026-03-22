package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_odometro", indexes = {
    @Index(name = "idx_ho_veiculo", columnList = "veiculo_id"),
    @Index(name = "idx_ho_data", columnList = "data_troca"),
    @Index(name = "idx_ho_delta", columnList = "delta_km")
})
public class HistoricoOdometro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "dispositivo_origem_id")
    private Long dispositivoOrigemId;

    @Column(name = "dispositivo_destino_id")
    private Long dispositivoDestinoId;

    @Column(name = "odometro_anterior_km", nullable = false)
    private Double odometroAnteriorKm;

    @Column(name = "odometro_novo_km", nullable = false)
    private Double odometroNovoKm;

    @Column(name = "delta_km", nullable = false)
    private Double deltaKm;

    @Column(name = "data_troca", nullable = false)
    private LocalDateTime dataTroca;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "alerta_inconsistencia")
    private Boolean alertaInconsistencia;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // Construtores
    public HistoricoOdometro() {}

    public HistoricoOdometro(Long veiculoId, Long dispositivoOrigemId, Long dispositivoDestinoId,
                             Double odometroAnteriorKm, Double odometroNovoKm, Double deltaKm,
                             LocalDateTime dataTroca, Long usuarioId, String observacao, Boolean alertaInconsistencia) {
        this.veiculoId = veiculoId;
        this.dispositivoOrigemId = dispositivoOrigemId;
        this.dispositivoDestinoId = dispositivoDestinoId;
        this.odometroAnteriorKm = odometroAnteriorKm;
        this.odometroNovoKm = odometroNovoKm;
        this.deltaKm = deltaKm;
        this.dataTroca = dataTroca;
        this.usuarioId = usuarioId;
        this.observacao = observacao;
        this.alertaInconsistencia = alertaInconsistencia;
    }

    // Builder pattern manual
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long veiculoId;
        private Long dispositivoOrigemId;
        private Long dispositivoDestinoId;
        private Double odometroAnteriorKm;
        private Double odometroNovoKm;
        private Double deltaKm;
        private LocalDateTime dataTroca;
        private Long usuarioId;
        private String observacao;
        private Boolean alertaInconsistencia;

        public Builder veiculoId(Long veiculoId) { this.veiculoId = veiculoId; return this; }
        public Builder dispositivoOrigemId(Long dispositivoOrigemId) { this.dispositivoOrigemId = dispositivoOrigemId; return this; }
        public Builder dispositivoDestinoId(Long dispositivoDestinoId) { this.dispositivoDestinoId = dispositivoDestinoId; return this; }
        public Builder odometroAnteriorKm(Double odometroAnteriorKm) { this.odometroAnteriorKm = odometroAnteriorKm; return this; }
        public Builder odometroNovoKm(Double odometroNovoKm) { this.odometroNovoKm = odometroNovoKm; return this; }
        public Builder deltaKm(Double deltaKm) { this.deltaKm = deltaKm; return this; }
        public Builder dataTroca(LocalDateTime dataTroca) { this.dataTroca = dataTroca; return this; }
        public Builder usuarioId(Long usuarioId) { this.usuarioId = usuarioId; return this; }
        public Builder observacao(String observacao) { this.observacao = observacao; return this; }
        public Builder alertaInconsistencia(Boolean alertaInconsistencia) { this.alertaInconsistencia = alertaInconsistencia; return this; }

        public HistoricoOdometro build() {
            return new HistoricoOdometro(veiculoId, dispositivoOrigemId, dispositivoDestinoId,
                    odometroAnteriorKm, odometroNovoKm, deltaKm, dataTroca, usuarioId, observacao, alertaInconsistencia);
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }

    public Long getDispositivoOrigemId() { return dispositivoOrigemId; }
    public void setDispositivoOrigemId(Long dispositivoOrigemId) { this.dispositivoOrigemId = dispositivoOrigemId; }

    public Long getDispositivoDestinoId() { return dispositivoDestinoId; }
    public void setDispositivoDestinoId(Long dispositivoDestinoId) { this.dispositivoDestinoId = dispositivoDestinoId; }

    public Double getOdometroAnteriorKm() { return odometroAnteriorKm; }
    public void setOdometroAnteriorKm(Double odometroAnteriorKm) { this.odometroAnteriorKm = odometroAnteriorKm; }

    public Double getOdometroNovoKm() { return odometroNovoKm; }
    public void setOdometroNovoKm(Double odometroNovoKm) { this.odometroNovoKm = odometroNovoKm; }

    public Double getDeltaKm() { return deltaKm; }
    public void setDeltaKm(Double deltaKm) { this.deltaKm = deltaKm; }

    public LocalDateTime getDataTroca() { return dataTroca; }
    public void setDataTroca(LocalDateTime dataTroca) { this.dataTroca = dataTroca; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Boolean getAlertaInconsistencia() { return alertaInconsistencia; }
    public void setAlertaInconsistencia(Boolean alertaInconsistencia) { this.alertaInconsistencia = alertaInconsistencia; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}