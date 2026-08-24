package com.telemetria.integration.sefaz.cte.status;

public class CteStatusResponse {

    private String sistema = "SEFAZ";
    private String documento = "CTE";
    private String ambiente;
    private String uf;
    private boolean disponivel;
    private String codigo;
    private String mensagem;
    private Long tempoRespostaMs;
    private boolean simulado;
    private String xmlEnvioSoap;
    private String xmlEnvioSoapBase64;
    private String xmlRetornoSoap;
    private String xmlRetornoSoapBase64;
    private String xmlRetornoDados;
    private String xmlRetornoDadosBase64;

    public CteStatusResponse() {
    }

    public CteStatusResponse(String ambiente, String uf, boolean disponivel, String codigo, String mensagem, Long tempoRespostaMs) {
        this.ambiente = ambiente;
        this.uf = uf;
        this.disponivel = disponivel;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public String getSistema() {
        return sistema;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Long getTempoRespostaMs() {
        return tempoRespostaMs;
    }

    public void setTempoRespostaMs(Long tempoRespostaMs) {
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public boolean isSimulado() {
        return simulado;
    }

    public void setSimulado(boolean simulado) {
        this.simulado = simulado;
    }

    public String getXmlEnvioSoap() {
        return xmlEnvioSoap;
    }

    public void setXmlEnvioSoap(String xmlEnvioSoap) {
        this.xmlEnvioSoap = xmlEnvioSoap;
    }

    public String getXmlEnvioSoapBase64() {
        return xmlEnvioSoapBase64;
    }

    public void setXmlEnvioSoapBase64(String xmlEnvioSoapBase64) {
        this.xmlEnvioSoapBase64 = xmlEnvioSoapBase64;
    }

    public String getXmlRetornoSoap() {
        return xmlRetornoSoap;
    }

    public void setXmlRetornoSoap(String xmlRetornoSoap) {
        this.xmlRetornoSoap = xmlRetornoSoap;
    }

    public String getXmlRetornoSoapBase64() {
        return xmlRetornoSoapBase64;
    }

    public void setXmlRetornoSoapBase64(String xmlRetornoSoapBase64) {
        this.xmlRetornoSoapBase64 = xmlRetornoSoapBase64;
    }

    public String getXmlRetornoDados() {
        return xmlRetornoDados;
    }

    public void setXmlRetornoDados(String xmlRetornoDados) {
        this.xmlRetornoDados = xmlRetornoDados;
    }

    public String getXmlRetornoDadosBase64() {
        return xmlRetornoDadosBase64;
    }

    public void setXmlRetornoDadosBase64(String xmlRetornoDadosBase64) {
        this.xmlRetornoDadosBase64 = xmlRetornoDadosBase64;
    }
}
