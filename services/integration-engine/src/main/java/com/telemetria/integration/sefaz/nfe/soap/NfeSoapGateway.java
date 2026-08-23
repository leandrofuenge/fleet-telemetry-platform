package com.telemetria.integration.sefaz.nfe.soap;

import java.net.URI;

import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.nfe.NfeProperties;

/** Orquestra envelope, transporte e validação do protocolo SOAP da NF-e. */
@Component
public class NfeSoapGateway {

    private final NfeSoapEnvelopeFactory envelopeFactory;
    private final NfeSoapTransport transport;
    private final NfeSoapResponseValidator responseValidator;
    private final NfeProperties properties;

    public NfeSoapGateway(NfeSoapEnvelopeFactory envelopeFactory, NfeSoapTransport transport,
            NfeSoapResponseValidator responseValidator, NfeProperties properties) {
        this.envelopeFactory = envelopeFactory;
        this.transport = transport;
        this.responseValidator = responseValidator;
        this.properties = properties;
    }

    public String enviar(NfeSoapService service, URI endpoint, String xmlFiscal) {
        String resposta = transport.enviar(envelopeFactory.criar(service, xmlFiscal), endpoint, service,
                properties.getTimeoutMillis());
        responseValidator.validar(resposta, service);
        return resposta;
    }
}
