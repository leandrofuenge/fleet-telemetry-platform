package com.telemetria.integration.sefaz.cte.soap;

/**
 * Mapeamento dos contratos e ações SOAP 1.2 para os WebServices do CT-e 4.00
 * em conformidade com o Manual de Orientação do Contribuinte (MOC).
 */
public enum CteSoapService {

    AUTORIZACAO("CTeRecepcaoSincV4", "cteRecepcao"),
    CONSULTA("CTeConsultaV4", "cteConsultaCT"),
    EVENTO("CTeRecepcaoEventoV4", "cteRecepcaoEvento"),
    STATUS("CTeStatusServicoV4", "cteStatusServicoCT");

    private static final String WSDL_BASE = "http://www.portalfiscal.inf.br/cte/wsdl/";

    private final String servico;
    private final String metodo;

    CteSoapService(String servico, String metodo) {
        this.servico = servico;
        this.metodo = metodo;
    }

    public String getServico() {
        return servico;
    }

    public String getMetodo() {
        return metodo;
    }

    /**
     * Retorna o Namespace XML do serviço SOAP.
     */
    public String namespace() {
        return WSDL_BASE + servico;
    }

    /**
     * Retorna o SOAPAction associado ao serviço.
     */
    public String soapAction() {
        return namespace() + "/" + metodo;
    }

    /**
     * Retorna o valor completo do Header HTTP Content-Type no padrão SOAP 1.2 (inclusão do parâmetro action).
     */
    public String getContentTypeHeader() {
        return "application/soap+xml; charset=utf-8; action=\"" + soapAction() + "\"";
    }
}
