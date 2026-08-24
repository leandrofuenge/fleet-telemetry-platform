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
@Entity @Getter @Setter @NoArgsConstructor @Table(name="ciot",indexes=@Index(name="idx_ciot_viagem",columnList="viagem_id")) public class Ciot {@Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;@Column(name="tenant_id",nullable=false)private Long tenantId;@Column(name="viagem_id",nullable=false)private Long viagemId;@Column(name="motorista_id",nullable=false)private Long motoristaId;@Column(name="codigo_ciot",nullable=false,length=26,unique=true)private String codigoCiot;@Column(name="valor_frete")private Double valorFrete;@Column(name="data_registro",nullable=false)private LocalDateTime dataRegistro=LocalDateTime.now();}
