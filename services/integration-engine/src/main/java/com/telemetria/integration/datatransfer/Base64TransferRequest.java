package com.telemetria.integration.datatransfer;

public class Base64TransferRequest {

    private String conteudo;
    private String conteudoBase64;
    private String tipoDocumento = "CTE";
    /** @deprecated Use entradaCompactadaGzip e compactarRespostaGzip. */
    @Deprecated
    private Boolean compactarGzip;
    private Boolean entradaCompactadaGzip;
    private Boolean compactarRespostaGzip;
    private boolean enveloparSoap = true;
    private boolean validarDocumentoXml = true;
    private boolean incluirConteudoNaResposta = false;

    public Base64TransferRequest() {
    }

    public Base64TransferRequest(String conteudo, String tipoDocumento, boolean compactarGzip, boolean enveloparSoap) {
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

    /**
     * Compatibilidade com clientes antigos: quando informado, aplica GZIP tanto
     * para a entrada quanto para a resposta. Novos clientes devem usar os campos específicos.
     */
    @Deprecated
    public boolean isCompactarGzip() {
        return Boolean.TRUE.equals(compactarGzip);
    }

    @Deprecated
    public void setCompactarGzip(boolean compactarGzip) {
        this.compactarGzip = compactarGzip;
    }

    public boolean isEntradaCompactadaGzip() {
        return entradaCompactadaGzip != null ? entradaCompactadaGzip : isCompactarGzip();
    }

    public void setEntradaCompactadaGzip(Boolean entradaCompactadaGzip) {
        this.entradaCompactadaGzip = entradaCompactadaGzip;
    }

    public boolean isCompactarRespostaGzip() {
        return compactarRespostaGzip != null ? compactarRespostaGzip : isCompactarGzip();
    }

    public void setCompactarRespostaGzip(Boolean compactarRespostaGzip) {
        this.compactarRespostaGzip = compactarRespostaGzip;
    }

    public boolean isEnveloparSoap() {
        return enveloparSoap;
    }

    public void setEnveloparSoap(boolean enveloparSoap) {
        this.enveloparSoap = enveloparSoap;
    }

    public boolean isValidarDocumentoXml() {
        return validarDocumentoXml;
    }

    public void setValidarDocumentoXml(boolean validarDocumentoXml) {
        this.validarDocumentoXml = validarDocumentoXml;
    }

    public boolean isIncluirConteudoNaResposta() {
        return incluirConteudoNaResposta;
    }

    public void setIncluirConteudoNaResposta(boolean incluirConteudoNaResposta) {
        this.incluirConteudoNaResposta = incluirConteudoNaResposta;
    }
}
