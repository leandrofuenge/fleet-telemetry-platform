package com.telemetria.domain.entity;

import java.time.LocalDateTime;

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
@Table(name = "ocorrencias", indexes = @Index(name = "idx_ocorrencia_tenant_status", columnList = "tenant_id,status"))
public class OcorrenciaOperacional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String descricao;
    private Double latitude;
    private Double longitude;
    @Column(name = "raio_impacto_m") private Double raioImpactoM;
    @Column(nullable = false) private String fonte;
    @Column(nullable = false) private String status = "ATIVA";
    @Column(name = "previsao_liberacao") private LocalDateTime previsaoLiberacao;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public String getTipo() { return tipo; }
    public void setTipo(String value) { tipo = value; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String value) { titulo = value; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double value) { latitude = value; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double value) { longitude = value; }
    public Double getRaioImpactoM() { return raioImpactoM; }
    public void setRaioImpactoM(Double value) { raioImpactoM = value; }
}
