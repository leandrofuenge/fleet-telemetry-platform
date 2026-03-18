package com.app.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.telemetria.domain.enums.StatusCertificado;
import com.app.telemetria.domain.enums.StatusDispositivo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dispositivos_iot", indexes = {
        @Index(name = "idx_disp_tenant", columnList = "tenant_id"),
        @Index(name = "idx_disp_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_disp_status", columnList = "status"),
        @Index(name = "idx_disp_imei", columnList = "imei")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispositivoIot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    @Column(name = "imei", length = 20)
    private String imei;

    @Column(name = "iccid", length = 25)
    private String iccid;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "fabricante", length = 100)
    private String fabricante;

    @Column(name = "modelo_hw", length = 100)
    private String modeloHw;

    @Column(name = "versao_firmware", length = 30)
    private String versaoFirmware;

    @Column(name = "versao_alvo", length = 30)
    private String versaoAlvo;

    @Column(name = "certificado_cn", length = 100)
    private String certificadoCn;

    @Column(name = "certificado_expira")
    private LocalDate certificadoExpira;

    @Column(name = "status_cert", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusCertificado statusCert = StatusCertificado.ATIVO;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusDispositivo status = StatusDispositivo.PENDENTE;

    @Column(name = "ultima_conexao")
    private LocalDateTime ultimaConexao;

    @Column(name = "ultimo_heartbeat")
    private LocalDateTime ultimoHeartbeat;

    @Column(name = "ip_ultima_conexao", length = 45)
    private String ipUltimaConexao;

    @Column(name = "tecnologia_rede", length = 10)
    private String tecnologiaRede;

    @Column(name = "rssi")
    private Double rssi;

    @Column(name = "freq_envio_s", nullable = false)
    @Builder.Default
    private Integer freqEnvioS = 5;

    @Column(name = "buffer_horas", nullable = false)
    @Builder.Default
    private Integer bufferHoras = 72;

    @Column(name = "tem_satelite", nullable = false)
    @Builder.Default
    private Boolean temSatelite = false;

    @Column(name = "iridium_imei", length = 20)
    private String iridiumImei;

    @Column(name = "satelite_ativo", nullable = false)
    @Builder.Default
    private Boolean sateliteAtivo = false;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "instalado_em")
    private LocalDate instaladoEm;

    @Column(name = "instalado_por", length = 100)
    private String instaladoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
