package com.telemetria.domain.entity;

import java.time.LocalDate;
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
@Table(name = "multas", indexes = @Index(name = "idx_multa_veiculo", columnList = "veiculo_id"))
public class Multa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "veiculo_id", nullable = false) private Long veiculoId;
    @Column(name = "motorista_imputado_id") private Long motoristaImputadoId;
    @Column(name = "auto_infracao", nullable = false, unique = true, length = 80) private String autoInfracao;
    @Column(name = "data_infracao", nullable = false) private LocalDateTime dataInfracao;
    private Double latitude;
    private Double longitude;
    @Column(name = "velocidade_registrada") private Double velocidadeRegistrada;
    @Column(name = "velocidade_telemetria") private Double velocidadeTelemetria;
    @Column(name = "imputacao_confirmada") private Boolean imputacaoConfirmada = false;
    @Column(name = "valor_original") private Double valorOriginal;
    @Column(name = "vencimento_normal") private LocalDate vencimentoNormal;
    @Column(nullable = false, length = 30) private String status = "PENDENTE";
    @Column(name = "contestacao_motivo", columnDefinition = "TEXT") private String contestacaoMotivo;
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long value) { veiculoId = value; }
    public String getAutoInfracao() { return autoInfracao; }
    public void setAutoInfracao(String value) { autoInfracao = value; }
}
