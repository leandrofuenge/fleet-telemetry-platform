package com.telemetria.integration.sefaz.cte.soap;

import com.telemetria.integration.util.SoapEnvelopeHelper;

/** Constrói envelopes SOAP 1.2 específicos dos contratos de CT-e. */
public final class CteSoapEnvelopeFactory {

    private static final String CTE_NAMESPACE = "http://www.portalfiscal.inf.br/cte";

    private CteSoapEnvelopeFactory() {
    }

    public static String wrap(String innerXml) {
        return SoapEnvelopeHelper.wrapInSoap12(innerXml, "cteDadosMsg", CTE_NAMESPACE);
    }

    public static String wrap(String innerXml, CteSoapService service) {
        if (service == null) {
            throw new IllegalArgumentException("O serviço SOAP do CT-e deve ser informado.");
        }
        return SoapEnvelopeHelper.wrapInSoap12(innerXml, "cteDadosMsg", service.namespace());
    }
}
