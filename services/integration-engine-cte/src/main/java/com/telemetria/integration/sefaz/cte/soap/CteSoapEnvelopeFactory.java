package com.telemetria.integration.sefaz.cte.soap;

import com.telemetria.integration.util.SoapEnvelopeHelper;

/**
 * Constrói envelopes SOAP 1.2 específicos
 * dos contratos de CT-e.
 */
public final class CteSoapEnvelopeFactory {

    private static final String ELEMENTO_DADOS =
            "cteDadosMsg";

    private CteSoapEnvelopeFactory() {
    }

    /**
     * Cria o envelope SOAP utilizando as informações
     * do serviço CT-e.
     */
    public static String wrap(
            String innerXml,
            CteSoapService service) {

        validarXml(innerXml);
        validarService(service);

        return SoapEnvelopeHelper.wrapInSoap12(
                innerXml,
                ELEMENTO_DADOS,
                service.namespace());
    }

    private static void validarXml(
            String innerXml) {

        if (innerXml == null
                || innerXml.isBlank()) {

            throw new IllegalArgumentException(
                    "O XML interno do CT-e não pode ser nulo ou vazio.");
        }
    }

    private static void validarService(
            CteSoapService service) {

        if (service == null) {

            throw new IllegalArgumentException(
                    "O serviço SOAP do CT-e deve ser informado.");
        }

        if (service.namespace() == null
                || service.namespace().isBlank()) {

            throw new IllegalStateException(
                    "Namespace SOAP não configurado "
                            + "para o serviço CT-e: "
                            + service);
        }
    }
}