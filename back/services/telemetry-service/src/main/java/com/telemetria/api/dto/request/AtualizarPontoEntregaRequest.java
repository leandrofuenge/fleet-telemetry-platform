package com.telemetria.api.dto.request;

import com.telemetria.domain.enums.StatusPontoEntrega;

import jakarta.validation.constraints.NotNull;

/**
 * RF11 — DTO para atualização de status do ponto de entrega
 */
public class AtualizarPontoEntregaRequest {

    @NotNull(message = "ID do ponto é obrigatório")
    private Long pontoEntregaId;

    @NotNull(message = "Novo status é obrigatório")
    private StatusPontoEntrega novoStatus;

    // RN-ENT-001: Proof of Delivery (assinatura ou foto obrigatório para ENTREGUE)
    private String assinaturaPath;
    private String fotoEntregaPath;

    // RN-ENT-001: Ocorrência obrigatória para status FALHOU
    private String ocorrencia;

    // Dados opcionais
    private Double latitudeChegada;
    private Double longitudeChegada;
    private Integer tempoPermanenciaMin;

    // Getters e Setters
    public Long getPontoEntregaId() {
        return pontoEntregaId;
    }

    public void setPontoEntregaId(Long pontoEntregaId) {
        this.pontoEntregaId = pontoEntregaId;
    }

    public StatusPontoEntrega getNovoStatus() {
        return novoStatus;
    }

    public void setNovoStatus(StatusPontoEntrega novoStatus) {
        this.novoStatus = novoStatus;
    }

    public String getAssinaturaPath() {
        return assinaturaPath;
    }

    public void setAssinaturaPath(String assinaturaPath) {
        this.assinaturaPath = assinaturaPath;
    }

    public String getFotoEntregaPath() {
        return fotoEntregaPath;
    }

    public void setFotoEntregaPath(String fotoEntregaPath) {
        this.fotoEntregaPath = fotoEntregaPath;
    }

    public String getOcorrencia() {
        return ocorrencia;
    }

    public void setOcorrencia(String ocorrencia) {
        this.ocorrencia = ocorrencia;
    }

    public Double getLatitudeChegada() {
        return latitudeChegada;
    }

    public void setLatitudeChegada(Double latitudeChegada) {
        this.latitudeChegada = latitudeChegada;
    }

    public Double getLongitudeChegada() {
        return longitudeChegada;
    }

    public void setLongitudeChegada(Double longitudeChegada) {
        this.longitudeChegada = longitudeChegada;
    }

    public Integer getTempoPermanenciaMin() {
        return tempoPermanenciaMin;
    }

    public void setTempoPermanenciaMin(Integer tempoPermanenciaMin) {
        this.tempoPermanenciaMin = tempoPermanenciaMin;
    }
}
