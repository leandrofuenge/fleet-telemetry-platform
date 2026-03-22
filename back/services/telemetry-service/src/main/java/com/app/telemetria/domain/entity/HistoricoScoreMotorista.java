package com.app.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "historico_score_motorista", indexes = {
    @Index(name = "idx_hsm_motorista", columnList = "motorista_id"),
    @Index(name = "idx_hsm_data", columnList = "data")
})
public class HistoricoScoreMotorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "motorista_id", nullable = false)
    private Long motoristaId;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "score_anterior", nullable = false)
    private Integer scoreAnterior;

    @Column(name = "score_novo", nullable = false)
    private Integer scoreNovo;

    @Column(name = "diferenca", nullable = false)
    private Integer diferenca;

    @Column(name = "motivo", length = 100)
    private String motivo;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "evento_tipo", length = 50)
    private String eventoTipo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private Motorista motorista;

    // Construtores
    public HistoricoScoreMotorista() {}

    public HistoricoScoreMotorista(Long motoristaId, LocalDate data, Integer scoreAnterior, 
                                   Integer scoreNovo, Integer diferenca, String motivo, 
                                   Long viagemId, String eventoTipo) {
        this.motoristaId = motoristaId;
        this.data = data;
        this.scoreAnterior = scoreAnterior;
        this.scoreNovo = scoreNovo;
        this.diferenca = diferenca;
        this.motivo = motivo;
        this.viagemId = viagemId;
        this.eventoTipo = eventoTipo;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Integer getScoreAnterior() { return scoreAnterior; }
    public void setScoreAnterior(Integer scoreAnterior) { this.scoreAnterior = scoreAnterior; }

    public Integer getScoreNovo() { return scoreNovo; }
    public void setScoreNovo(Integer scoreNovo) { this.scoreNovo = scoreNovo; }

    public Integer getDiferenca() { return diferenca; }
    public void setDiferenca(Integer diferenca) { this.diferenca = diferenca; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getViagemId() { return viagemId; }
    public void setViagemId(Long viagemId) { this.viagemId = viagemId; }

    public String getEventoTipo() { return eventoTipo; }
    public void setEventoTipo(String eventoTipo) { this.eventoTipo = eventoTipo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
}