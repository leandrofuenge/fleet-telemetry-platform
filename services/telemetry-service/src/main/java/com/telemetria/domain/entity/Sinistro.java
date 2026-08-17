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
@Table(name = "sinistros", indexes = @Index(name = "idx_sinistro_tenant", columnList = "tenant_id"))
public class Sinistro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "veiculo_id", nullable = false) private Long veiculoId;
    @Column(name = "viagem_id") private Long viagemId;
    @Column(name = "motorista_id") private Long motoristaId;
    @Column(nullable = false, length = 30) private String tipo;
    @Column(nullable = false, length = 30) private String status = "ABERTO";
    @Column(name = "data_hora_ocorrencia", nullable = false) private LocalDateTime dataHoraOcorrencia;
    private Double latitude;
    private Double longitude;
    @Column(name = "velocidade_no_momento") private Double velocidadeNoMomento;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "telemetria_snapshot", columnDefinition = "json")
    private Map<String, Object> telemetriaSnapshot;
    @Column(name = "preservar_dados", nullable = false) private Boolean preservarDados = true;
    public void setTenantId(Long value) { tenantId = value; }
    public void setVeiculoId(Long value) { veiculoId = value; }
    public void setViagemId(Long value) { viagemId = value; }
    public void setMotoristaId(Long value) { motoristaId = value; }
    public void setTipo(String value) { tipo = value; }
    public void setDataHoraOcorrencia(LocalDateTime value) { dataHoraOcorrencia = value; }
    public void setLatitude(Double value) { latitude = value; }
    public void setLongitude(Double value) { longitude = value; }
    public void setVelocidadeNoMomento(Double value) { velocidadeNoMomento = value; }
    public void setTelemetriaSnapshot(Map<String, Object> value) { telemetriaSnapshot = value; }
}
