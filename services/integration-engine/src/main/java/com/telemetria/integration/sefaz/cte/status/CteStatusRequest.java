package com.telemetria.integration.sefaz.cte.status;

public class CteStatusRequest {

    private String uf;
    private String ambiente;

    public CteStatusRequest() {
    }

    public CteStatusRequest(String uf, String ambiente) {
        this.uf = uf;
        this.ambiente = ambiente;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }
}
