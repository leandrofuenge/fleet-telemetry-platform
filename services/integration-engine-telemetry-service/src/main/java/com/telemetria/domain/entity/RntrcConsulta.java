package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "rntrc_consultas", indexes = @Index(name = "idx_rntrc_numero", columnList = "rntrc"))
public class RntrcConsulta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 30) private String rntrc;
    @Column(nullable = false, length = 20) private String situacao;
    @Column(name = "data_consulta", nullable = false) private LocalDateTime dataConsulta = LocalDateTime.now();
    @Column(name = "expira_em", nullable = false) private LocalDateTime expiraEm = LocalDateTime.now().plusHours(24);
    public Long getId() { return id; } public void setId(Long value) { id = value; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long value) { tenantId = value; }
    public String getRntrc() { return rntrc; } public void setRntrc(String value) { rntrc = value; }
    public String getSituacao() { return situacao; } public void setSituacao(String value) { situacao = value; }
    public LocalDateTime getDataConsulta() { return dataConsulta; } public void setDataConsulta(LocalDateTime value) { dataConsulta = value; }
    public LocalDateTime getExpiraEm() { return expiraEm; } public void setExpiraEm(LocalDateTime value) { expiraEm = value; }
}
