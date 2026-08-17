package com.telemetria.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "cte", indexes = @Index(name = "idx_cte_chave", columnList = "chave_cte", unique = true))
public class Cte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "carga_id", nullable = false) private Long cargaId;
    @Column(name = "chave_cte", nullable = false, length = 44) private String chaveCte;
    @Column(nullable = false, length = 20) private String status = "AUTORIZADO";
    @Column(name = "valor_total") private Double valorTotal;
    @Column(name = "peso_kg") private Double pesoKg;
    public Long getId() { return id; } public void setId(Long value) { id = value; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long value) { tenantId = value; }
    public Long getCargaId() { return cargaId; } public void setCargaId(Long value) { cargaId = value; }
    public String getChaveCte() { return chaveCte; } public void setChaveCte(String value) { chaveCte = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public Double getValorTotal() { return valorTotal; } public void setValorTotal(Double value) { valorTotal = value; }
    public Double getPesoKg() { return pesoKg; } public void setPesoKg(Double value) { pesoKg = value; }
}
