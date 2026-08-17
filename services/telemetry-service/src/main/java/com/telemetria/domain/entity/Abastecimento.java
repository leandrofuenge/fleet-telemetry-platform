package com.telemetria.domain.entity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
@Entity @Table(name="abastecimentos", indexes={@Index(name="idx_aba_tenant_data",columnList="tenant_id, data_hora"),@Index(name="idx_aba_veiculo",columnList="veiculo_id")})
public class Abastecimento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="tenant_id",nullable=false) private Long tenantId; @Column(name="veiculo_id",nullable=false) private Long veiculoId; @Column(name="motorista_id") private Long motoristaId; @Column(name="data_hora",nullable=false) private LocalDateTime dataHora;
 @Column(nullable=false) private Double litros; @Column(name="valor_total",nullable=false) private Double valorTotal; @Column private Double odometro; @Column(name="posto_cnpj",length=14) private String postoCnpj; @Column(name="tipo_combustivel",length=30) private String tipoCombustivel; @Column(name="tipo_origem",nullable=false,length=20) private String tipoOrigem="MANUAL"; @Column(name="litros_sensor") private Double litrosSensor; @Column(name="fraude_score",nullable=false) private Integer fraudeScore=0; @Column(name="posto_autorizado") private Boolean postoAutorizado=true; @Column(name="status_conciliacao",nullable=false,length=20) private String statusConciliacao="PENDENTE";
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getVeiculoId(){return veiculoId;} public void setVeiculoId(Long v){veiculoId=v;} public Long getMotoristaId(){return motoristaId;} public void setMotoristaId(Long v){motoristaId=v;} public LocalDateTime getDataHora(){return dataHora;} public void setDataHora(LocalDateTime v){dataHora=v;} public Double getLitros(){return litros;} public void setLitros(Double v){litros=v;} public Double getValorTotal(){return valorTotal;} public void setValorTotal(Double v){valorTotal=v;} public Double getOdometro(){return odometro;} public void setOdometro(Double v){odometro=v;} public String getPostoCnpj(){return postoCnpj;} public void setPostoCnpj(String v){postoCnpj=v;} public String getTipoCombustivel(){return tipoCombustivel;} public void setTipoCombustivel(String v){tipoCombustivel=v;} public String getTipoOrigem(){return tipoOrigem;} public void setTipoOrigem(String v){tipoOrigem=v;} public Double getLitrosSensor(){return litrosSensor;} public void setLitrosSensor(Double v){litrosSensor=v;} public Integer getFraudeScore(){return fraudeScore;} public void setFraudeScore(Integer v){fraudeScore=v;} public Boolean getPostoAutorizado(){return postoAutorizado;} public void setPostoAutorizado(Boolean v){postoAutorizado=v;} public String getStatusConciliacao(){return statusConciliacao;} public void setStatusConciliacao(String v){statusConciliacao=v;}
}
