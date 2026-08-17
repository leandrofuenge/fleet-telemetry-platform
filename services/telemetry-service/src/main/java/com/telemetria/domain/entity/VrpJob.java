package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "vrp_jobs", indexes = @Index(name = "idx_vrp_tenant_status", columnList = "tenant_id,status"))
public class VrpJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String status = "PENDENTE";
    @Column(name = "tipo_vrp", nullable = false) private String tipoVrp;
    @Column(name = "num_veiculos") private Integer numVeiculos;
    @Column(name = "num_pontos") private Integer numPontos;
    @Column(name = "solver_usado") private String solverUsado;
    @Column(name = "tempo_execucao_ms") private Long tempoExecucaoMs;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plano_resultado", columnDefinition = "json")
    private Map<String, Object> planoResultado;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getTipoVrp() { return tipoVrp; }
    public void setTipoVrp(String value) { tipoVrp = value; }
    public Integer getNumVeiculos() { return numVeiculos; }
    public void setNumVeiculos(Integer value) { numVeiculos = value; }
    public Integer getNumPontos() { return numPontos; }
    public void setNumPontos(Integer value) { numPontos = value; }
    public String getSolverUsado() { return solverUsado; }
    public void setSolverUsado(String value) { solverUsado = value; }
    public Long getTempoExecucaoMs() { return tempoExecucaoMs; }
    public void setTempoExecucaoMs(Long value) { tempoExecucaoMs = value; }
    public Map<String, Object> getPlanoResultado() { return planoResultado; }
    public void setPlanoResultado(Map<String, Object> value) { planoResultado = value; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime value) { criadoEm = value; }
}
