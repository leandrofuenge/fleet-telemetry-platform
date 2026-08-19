package com.telemetria.integration.sefaz.cte;

/**
 * Contrato SOAP 1.2 dos serviços CT-e 4.00.
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

    public String namespace() {
        return WSDL_BASE + servico;
    }

    public String soapAction() {
        return namespace() + "/" + metodo;
    }
}
