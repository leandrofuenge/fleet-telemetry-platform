package com.telemetria.integration.sefaz.nfe.soap;

import org.springframework.stereotype.Component;

/** Monta envelopes SOAP 1.2 sem duplicar declarações XML no corpo fiscal. */
@Component
public class NfeSoapEnvelopeFactory {

    public String criar(NfeSoapService service, String xmlFiscal) {
        if (service == null) {
            throw new IllegalArgumentException("O serviço SOAP NF-e é obrigatório.");
        }
        if (xmlFiscal == null || xmlFiscal.isBlank()) {
            throw new IllegalArgumentException("O XML fiscal NF-e é obrigatório.");
        }
        String conteudo = xmlFiscal.replaceFirst("^\\s*<\\?xml[^>]*\\?>\\s*", "");
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body>"
                + "<nfeDadosMsg xmlns=\"" + service.namespace() + "\">" + conteudo
                + "</nfeDadosMsg></soap12:Body></soap12:Envelope>";
    }
}
