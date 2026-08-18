package com.telemetria.integration.workflow.domain;

public class ViagemWorkflowRequest {

    private String viagemId;
    private String veiculoPlaca;
    private String motoristaCpf;
    private String ufOrigem;
    private String ufDestino;
    private String renavam;
    private String transportadorDocumento;

    public ViagemWorkflowRequest() {
    }

    public ViagemWorkflowRequest(String viagemId, String veiculoPlaca, String motoristaCpf, String ufOrigem, String ufDestino) {
        this.viagemId = viagemId;
        this.veiculoPlaca = veiculoPlaca;
        this.motoristaCpf = motoristaCpf;
        this.ufOrigem = ufOrigem;
        this.ufDestino = ufDestino;
    }

    public ViagemWorkflowRequest(String viagemId, String veiculoPlaca, String motoristaCpf, String ufOrigem,
            String ufDestino, String renavam, String transportadorDocumento) {
        this(viagemId, veiculoPlaca, motoristaCpf, ufOrigem, ufDestino);
        this.renavam = renavam;
        this.transportadorDocumento = transportadorDocumento;
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

    public String getRenavam() {
        return renavam;
    }

    public void setRenavam(String renavam) {
        this.renavam = renavam;
    }

    public String getTransportadorDocumento() {
        return transportadorDocumento;
    }

    public void setTransportadorDocumento(String transportadorDocumento) {
        this.transportadorDocumento = transportadorDocumento;
    }
}
