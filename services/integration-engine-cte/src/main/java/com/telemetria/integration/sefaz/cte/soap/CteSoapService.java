package com.telemetria.integration.sefaz.cte.soap;

import java.time.Duration;

/**
 * Serviços SOAP utilizados na integração CT-e 4.00.
 *
 * <p>
 * Cada serviço concentra suas características técnicas:
 * </p>
 *
 * <ul>
 *     <li>namespace SOAP;</li>
 *     <li>SOAP Action;</li>
 *     <li>timeout recomendado.</li>
 * </ul>
 */
public enum CteSoapService {

    STATUS(
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeStatusServicoV4",
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeStatusServicoV4/cteStatusServicoCT",
            Duration.ofSeconds(10)),

    AUTORIZACAO(
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeRecepcaoSincV4",
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeRecepcaoSincV4/cteRecepcao",
            Duration.ofSeconds(30)),

    CONSULTA(
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeConsultaV4",
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeConsultaV4/cteConsultaCT",
            Duration.ofSeconds(15)),

    EVENTO(
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeRecepcaoEventoV4",
            "http://www.portalfiscal.inf.br/cte/wsdl/CTeRecepcaoEventoV4/cteRecepcaoEvento",
            Duration.ofSeconds(20));

    private static final String SOAP_12_CONTENT_TYPE =
            "application/soap+xml; charset=utf-8; action=\"%s\"";

    private final String namespace;
    private final String soapAction;
    private final Duration timeout;

    CteSoapService(
            String namespace,
            String soapAction,
            Duration timeout) {

        this.namespace = namespace;
        this.soapAction = soapAction;
        this.timeout = timeout;
    }

    public String namespace() {
        return namespace;
    }

    public String soapAction() {
        return soapAction;
    }

    public Duration timeout() {
        return timeout;
    }

    public String contentType() {
        return SOAP_12_CONTENT_TYPE.formatted(soapAction);
    }
}
