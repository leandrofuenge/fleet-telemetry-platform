package com.telemetria.infrastructure.integration.engine.dto;

public class IntegrationViagemWorkflowRequest {

    private String viagemId;
    private String veiculoPlaca;
    private String motoristaCpf;
    private String ufOrigem;
    private String ufDestino;

    public IntegrationViagemWorkflowRequest() {
    }

    public IntegrationViagemWorkflowRequest(String viagemId, String veiculoPlaca, String motoristaCpf, String ufOrigem, String ufDestino) {
        this.viagemId = viagemId;
        this.veiculoPlaca = veiculoPlaca;
        this.motoristaCpf = motoristaCpf;
        this.ufOrigem = ufOrigem;
        this.ufDestino = ufDestino;
    }

    public String getViagemId() {
        return viagemId;
    }

    public void setViagemId(String viagemId) {
        this.viagemId = viagemId;
    }

    public String getVeiculoPlaca() {
        return veiculoPlaca;
    }

    public void setVeiculoPlaca(String veiculoPlaca) {
        this.veiculoPlaca = veiculoPlaca;
    }

    public String getMotoristaCpf() {
        return motoristaCpf;
    }

    public void setMotoristaCpf(String motoristaCpf) {
        this.motoristaCpf = motoristaCpf;
    }

    public String getUfOrigem() {
        return ufOrigem;
    }

    public void setUfOrigem(String ufOrigem) {
        this.ufOrigem = ufOrigem;
    }

    public String getUfDestino() {
        return ufDestino;
    }

    public void setUfDestino(String ufDestino) {
        this.ufDestino = ufDestino;
    }
}
