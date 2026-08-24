package com.telemetria.integration.nfe.soap;

/** Catálogo dos contratos SOAP 1.2 da NF-e 4.00. */
public enum NfeSoapService {
    AUTORIZACAO("NfeAutorizacao4", "nfeAutorizacaoLote"),
    RET_AUTORIZACAO("NfeRetAutorizacao4", "nfeRetAutorizacaoLote"),
    CONSULTA("NfeConsultaProtocolo4", "nfeConsultaNF"),
    STATUS("NfeStatusServico4", "nfeStatusServicoNF"),
    EVENTO("NFeRecepcaoEvento4", "nfeRecepcaoEvento"),
    INUTILIZACAO("NfeInutilizacao4", "nfeInutilizacaoNF"),
    DISTRIBUICAO_DFE("NFeDistribuicaoDFe", "nfeDistDFeInteresse");

    private static final String WSDL_BASE = "http://www.portalfiscal.inf.br/nfe/wsdl/";

    private final String servico;
    private final String metodo;

    NfeSoapService(String servico, String metodo) {
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
