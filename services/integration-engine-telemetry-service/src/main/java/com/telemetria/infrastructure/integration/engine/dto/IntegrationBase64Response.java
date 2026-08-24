package com.telemetria.infrastructure.integration.engine.dto;

public class IntegrationBase64Response {

    private boolean sucesso;
    private String tipoDocumento;
    private String conteudoOriginal;
    private String conteudoBase64;
    private String soapEnvelopeXml;
    private String soapEnvelopeXmlBase64;
    private int tamanhoBytesOriginal;
    private int tamanhoBytesBase64;
    private boolean compactadoGzip;
    private String mensagem;

    public IntegrationBase64Response() {
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getConteudoOriginal() {
        return conteudoOriginal;
    }

    public void setConteudoOriginal(String conteudoOriginal) {
        this.conteudoOriginal = conteudoOriginal;
    }

    public String getConteudoBase64() {
        return conteudoBase64;
    }

    public void setConteudoBase64(String conteudoBase64) {
        this.conteudoBase64 = conteudoBase64;
    }

    public String getSoapEnvelopeXml() {
        return soapEnvelopeXml;
    }

    public void setSoapEnvelopeXml(String soapEnvelopeXml) {
        this.soapEnvelopeXml = soapEnvelopeXml;
    }

    public String getSoapEnvelopeXmlBase64() {
        return soapEnvelopeXmlBase64;
    }

    public void setSoapEnvelopeXmlBase64(String soapEnvelopeXmlBase64) {
        this.soapEnvelopeXmlBase64 = soapEnvelopeXmlBase64;
    }

    public int getTamanhoBytesOriginal() {
        return tamanhoBytesOriginal;
    }

    public void setTamanhoBytesOriginal(int tamanhoBytesOriginal) {
        this.tamanhoBytesOriginal = tamanhoBytesOriginal;
    }

    public int getTamanhoBytesBase64() {
        return tamanhoBytesBase64;
    }

    public void setTamanhoBytesBase64(int tamanhoBytesBase64) {
        this.tamanhoBytesBase64 = tamanhoBytesBase64;
    }

    public boolean isCompactadoGzip() {
        return compactadoGzip;
    }

    public void setCompactadoGzip(boolean compactadoGzip) {
        this.compactadoGzip = compactadoGzip;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
