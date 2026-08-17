package com.telemetria.infrastructure.integration.engine.dto;

public class IntegrationBase64Request {

    private String conteudo;
    private String conteudoBase64;
    private String tipoDocumento = "CTE";
    private boolean compactarGzip = false;
    private boolean enveloparSoap = true;

    public IntegrationBase64Request() {
    }

    public IntegrationBase64Request(String conteudo, String tipoDocumento, boolean compactarGzip, boolean enveloparSoap) {
        this.conteudo = conteudo;
        this.tipoDocumento = tipoDocumento;
        this.compactarGzip = compactarGzip;
        this.enveloparSoap = enveloparSoap;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getConteudoBase64() {
        return conteudoBase64;
    }

    public void setConteudoBase64(String conteudoBase64) {
        this.conteudoBase64 = conteudoBase64;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public boolean isCompactarGzip() {
        return compactarGzip;
    }

    public void setCompactarGzip(boolean compactarGzip) {
        this.compactarGzip = compactarGzip;
    }

    public boolean isEnveloparSoap() {
        return enveloparSoap;
    }

    public void setEnveloparSoap(boolean enveloparSoap) {
        this.enveloparSoap = enveloparSoap;
    }
}
