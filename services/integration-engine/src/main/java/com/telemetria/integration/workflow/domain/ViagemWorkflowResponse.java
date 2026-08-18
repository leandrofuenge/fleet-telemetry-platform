package com.telemetria.integration.workflow.domain;

import java.util.ArrayList;
import java.util.List;

public class ViagemWorkflowResponse {

    private String viagemId;
    private String status; // LIBERADA, BLOQUEADA, PENDENTE
    private boolean motoristaValido;
    private boolean veiculoValido;
    private boolean sefazDisponivel;
    private boolean anttRegular;
    private List<String> etapasConcluidas = new ArrayList<>();
    private List<String> pendencias = new ArrayList<>();

    public ViagemWorkflowResponse() {
    }

    public ViagemWorkflowResponse(String viagemId, String status) {
        this.viagemId = viagemId;
        this.status = status;
    }

    public String getViagemId() {
        return viagemId;
    }

    public void setViagemId(String viagemId) {
        this.viagemId = viagemId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isMotoristaValido() {
        return motoristaValido;
    }

    public void setMotoristaValido(boolean motoristaValido) {
        this.motoristaValido = motoristaValido;
    }

    public boolean isVeiculoValido() {
        return veiculoValido;
    }

    public void setVeiculoValido(boolean veiculoValido) {
        this.veiculoValido = veiculoValido;
    }

    public boolean isSefazDisponivel() {
        return sefazDisponivel;
    }

    public void setSefazDisponivel(boolean sefazDisponivel) {
        this.sefazDisponivel = sefazDisponivel;
    }

    public boolean isAnttRegular() {
        return anttRegular;
    }

    public void setAnttRegular(boolean anttRegular) {
        this.anttRegular = anttRegular;
    }

    public List<String> getEtapasConcluidas() {
        return etapasConcluidas;
    }

    public void setEtapasConcluidas(List<String> etapasConcluidas) {
        this.etapasConcluidas = etapasConcluidas;
    }

    public List<String> getPendencias() {
        return pendencias;
    }

    public void setPendencias(List<String> pendencias) {
        this.pendencias = pendencias;
    }
}
