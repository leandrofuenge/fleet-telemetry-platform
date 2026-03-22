package com.app.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.telemetria.domain.enums.StatusCertificado;
import com.app.telemetria.domain.enums.StatusDispositivo;
import com.app.telemetria.domain.enums.TipoDispositivo;

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
@Table(name = "dispositivos_iot", indexes = {
        @Index(name = "idx_disp_tenant", columnList = "tenant_id"),
        @Index(name = "idx_disp_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_disp_status", columnList = "status"),
        @Index(name = "idx_disp_imei", columnList = "imei")
})
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

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoDispositivo tipo;

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
    private StatusCertificado statusCert = StatusCertificado.ATIVO;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
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
    private Integer freqEnvioS = 5;

    @Column(name = "buffer_horas", nullable = false)
    private Integer bufferHoras = 72;

    @Column(name = "tem_satelite", nullable = false)
    private Boolean temSatelite = false;

    @Column(name = "iridium_imei", length = 20)
    private String iridiumImei;

    @Column(name = "satelite_ativo", nullable = false)
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

    // Construtores
    public DispositivoIot() {}

    public DispositivoIot(String deviceId, Long tenantId, Long veiculoId) {
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.veiculoId = veiculoId;
        this.status = StatusDispositivo.PENDENTE;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public TipoDispositivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoDispositivo tipo) {
        this.tipo = tipo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public String getModeloHw() {
        return modeloHw;
    }

    public void setModeloHw(String modeloHw) {
        this.modeloHw = modeloHw;
    }

    public String getVersaoFirmware() {
        return versaoFirmware;
    }

    public void setVersaoFirmware(String versaoFirmware) {
        this.versaoFirmware = versaoFirmware;
    }

    public String getVersaoAlvo() {
        return versaoAlvo;
    }

    public void setVersaoAlvo(String versaoAlvo) {
        this.versaoAlvo = versaoAlvo;
    }

    public String getCertificadoCn() {
        return certificadoCn;
    }

    public void setCertificadoCn(String certificadoCn) {
        this.certificadoCn = certificadoCn;
    }

    public LocalDate getCertificadoExpira() {
        return certificadoExpira;
    }

    public void setCertificadoExpira(LocalDate certificadoExpira) {
        this.certificadoExpira = certificadoExpira;
    }

    public StatusCertificado getStatusCert() {
        return statusCert;
    }

    public void setStatusCert(StatusCertificado statusCert) {
        this.statusCert = statusCert;
    }

    public StatusDispositivo getStatus() {
        return status;
    }

    public void setStatus(StatusDispositivo status) {
        this.status = status;
    }

    public LocalDateTime getUltimaConexao() {
        return ultimaConexao;
    }

    public void setUltimaConexao(LocalDateTime ultimaConexao) {
        this.ultimaConexao = ultimaConexao;
    }

    public LocalDateTime getUltimoHeartbeat() {
        return ultimoHeartbeat;
    }

    public void setUltimoHeartbeat(LocalDateTime ultimoHeartbeat) {
        this.ultimoHeartbeat = ultimoHeartbeat;
    }

    public String getIpUltimaConexao() {
        return ipUltimaConexao;
    }

    public void setIpUltimaConexao(String ipUltimaConexao) {
        this.ipUltimaConexao = ipUltimaConexao;
    }

    public String getTecnologiaRede() {
        return tecnologiaRede;
    }

    public void setTecnologiaRede(String tecnologiaRede) {
        this.tecnologiaRede = tecnologiaRede;
    }

    public Double getRssi() {
        return rssi;
    }

    public void setRssi(Double rssi) {
        this.rssi = rssi;
    }

    public Integer getFreqEnvioS() {
        return freqEnvioS;
    }

    public void setFreqEnvioS(Integer freqEnvioS) {
        this.freqEnvioS = freqEnvioS;
    }

    public Integer getBufferHoras() {
        return bufferHoras;
    }

    public void setBufferHoras(Integer bufferHoras) {
        this.bufferHoras = bufferHoras;
    }

    public Boolean getTemSatelite() {
        return temSatelite;
    }

    public void setTemSatelite(Boolean temSatelite) {
        this.temSatelite = temSatelite;
    }

    public String getIridiumImei() {
        return iridiumImei;
    }

    public void setIridiumImei(String iridiumImei) {
        this.iridiumImei = iridiumImei;
    }

    public Boolean getSateliteAtivo() {
        return sateliteAtivo;
    }

    public void setSateliteAtivo(Boolean sateliteAtivo) {
        this.sateliteAtivo = sateliteAtivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDate getInstaladoEm() {
        return instaladoEm;
    }

    public void setInstaladoEm(LocalDate instaladoEm) {
        this.instaladoEm = instaladoEm;
    }

    public String getInstaladoPor() {
        return instaladoPor;
    }

    public void setInstaladoPor(String instaladoPor) {
        this.instaladoPor = instaladoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}