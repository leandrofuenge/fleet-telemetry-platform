package com.telemetria.integration.nfe.soap;

/**
 * Catálogo das operações SOAP 1.2 utilizadas na NF-e 4.00.
 *
 * O namespace é utilizado na construção do XML/SOAP e na definição
 * do SOAPAction. Ele não representa o endpoint HTTP da SEFAZ.
 */
public enum NfeSoapService {

    AUTORIZACAO(
            "NfeAutorizacao4",
            "nfeAutorizacaoLote",
            false,
            "nfeResultMsg",
            null
    ),

    RET_AUTORIZACAO(
            "NfeRetAutorizacao4",
            "nfeRetAutorizacaoLote",
            false,
            "nfeResultMsg",
            null
    ),

    CONSULTA(
            "NfeConsultaProtocolo4",
            "nfeConsultaNF",
            false,
            "nfeResultMsg",
            null
    ),

    STATUS(
            "NfeStatusServico4",
            "nfeStatusServicoNF",
            false,
            "nfeResultMsg",
            null
    ),

    EVENTO(
            "NFeRecepcaoEvento4",
            "nfeRecepcaoEvento",
            false,
            "nfeResultMsg",
            null
    ),

    INUTILIZACAO(
            "NfeInutilizacao4",
            "nfeInutilizacaoNF",
            false,
            "nfeResultMsg",
            null
    ),

    DISTRIBUICAO_DFE(
            "NFeDistribuicaoDFe",
            "nfeDistDFeInteresse",
            true,
            "nfeDistDFeInteresseResponse",
            "nfeDistDFeInteresseResult"
    );

    private static final String NAMESPACE_BASE =
            "http://www.portalfiscal.inf.br/nfe/wsdl/";

    private final String servico;
    private final String metodo;
    private final boolean requisicaoEncapsuladaPeloMetodo;
    private final String elementoResposta;
    private final String elementoResultado;

    NfeSoapService(
            String servico,
            String metodo,
            boolean requisicaoEncapsuladaPeloMetodo,
            String elementoResposta,
            String elementoResultado) {

        this.servico = servico;
        this.metodo = metodo;
        this.requisicaoEncapsuladaPeloMetodo = requisicaoEncapsuladaPeloMetodo;
        this.elementoResposta = elementoResposta;
        this.elementoResultado = elementoResultado;
    }

    /**
     * Retorna o namespace SOAP da operação.
     */
    public String namespace() {
        return NAMESPACE_BASE + servico;
    }

    /**
     * Retorna o SOAPAction da operação.
     */
    public String soapAction() {
        return namespace() + "/" + metodo;
    }

    public String servico() {
        return servico;
    }

    public String metodo() {
        return metodo;
    }

    public boolean requisicaoEncapsuladaPeloMetodo() {
        return requisicaoEncapsuladaPeloMetodo;
    }

    public String elementoResposta() {
        return elementoResposta;
    }

    public String elementoResultado() {
        return elementoResultado;
    }
}
