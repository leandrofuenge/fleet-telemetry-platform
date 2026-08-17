package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import com.telemetria.domain.enums.StatusEscala;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "escalas_motorista", indexes = {@Index(name="idx_esc_motorista_inicio", columnList="motorista_id, data_inicio_turno"), @Index(name="idx_esc_veiculo_inicio", columnList="veiculo_id, data_inicio_turno")})
public class EscalaMotorista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="tenant_id", nullable=false) private Long tenantId;
    @Column(name="motorista_id", nullable=false) private Long motoristaId;
    @Column(name="veiculo_id", nullable=false) private Long veiculoId;
    @Column(name="rota_id") private Long rotaId;
    @Column(name="data_inicio_turno", nullable=false) private LocalDateTime dataInicioTurno;
    @Column(name="data_fim_turno", nullable=false) private LocalDateTime dataFimTurno;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private StatusEscala status = StatusEscala.PLANEJADA;
    @Column(name="confirmado_motorista", nullable=false) private Boolean confirmadoMotorista = false;
    @Column(name="motivo_cancelamento", columnDefinition="TEXT") private String motivoCancelamento;
    @Column(name="criado_por", length=36) private String criadoPor;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getMotoristaId(){return motoristaId;} public void setMotoristaId(Long v){motoristaId=v;} public Long getVeiculoId(){return veiculoId;} public void setVeiculoId(Long v){veiculoId=v;}
    public Long getRotaId(){return rotaId;} public void setRotaId(Long v){rotaId=v;} public LocalDateTime getDataInicioTurno(){return dataInicioTurno;} public void setDataInicioTurno(LocalDateTime v){dataInicioTurno=v;}
    public LocalDateTime getDataFimTurno(){return dataFimTurno;} public void setDataFimTurno(LocalDateTime v){dataFimTurno=v;} public StatusEscala getStatus(){return status;} public void setStatus(StatusEscala v){status=v;}
    public Boolean getConfirmadoMotorista(){return confirmadoMotorista;} public void setConfirmadoMotorista(Boolean v){confirmadoMotorista=v;} public String getMotivoCancelamento(){return motivoCancelamento;} public void setMotivoCancelamento(String v){motivoCancelamento=v;}
    public String getCriadoPor(){return criadoPor;} public void setCriadoPor(String v){criadoPor=v;}
}
