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
@Table(name = "mdfe", indexes = {@Index(name = "idx_mdfe_viagem", columnList = "viagem_id"), @Index(name = "idx_mdfe_chave", columnList = "chave_mdfe", unique = true)})
public class Mdfe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "viagem_id", nullable = false) private Long viagemId;
    @Column(name = "chave_mdfe", length = 44) private String chaveMdfe;
    @Column(nullable = false, length = 20) private String status = "PENDENTE";
    @Column(name = "protocolo_autorizacao", length = 100) private String protocoloAutorizacao;
    @Column(name = "data_emissao") private LocalDateTime dataEmissao;
    @Column(name = "data_encerramento") private LocalDateTime dataEncerramento;
    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT") private String motivoCancelamento;
    @Column(name = "contingencia", nullable = false) private Boolean contingencia = false;
    public Long getId() { return id; } public void setId(Long value) { id = value; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long value) { tenantId = value; }
    public Long getViagemId() { return viagemId; } public void setViagemId(Long value) { viagemId = value; }
    public String getChaveMdfe() { return chaveMdfe; } public void setChaveMdfe(String value) { chaveMdfe = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public String getProtocoloAutorizacao() { return protocoloAutorizacao; } public void setProtocoloAutorizacao(String value) { protocoloAutorizacao = value; }
    public LocalDateTime getDataEmissao() { return dataEmissao; } public void setDataEmissao(LocalDateTime value) { dataEmissao = value; }
    public LocalDateTime getDataEncerramento() { return dataEncerramento; } public void setDataEncerramento(LocalDateTime value) { dataEncerramento = value; }
    public String getMotivoCancelamento() { return motivoCancelamento; } public void setMotivoCancelamento(String value) { motivoCancelamento = value; }
    public Boolean getContingencia() { return contingencia; } public void setContingencia(Boolean value) { contingencia = value; }
}
