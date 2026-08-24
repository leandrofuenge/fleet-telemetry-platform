package com.telemetria.integration.sefaz.nfe.soap;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.nfe.NfeProperties;

/** Orquestra envelope, transporte e validação do protocolo SOAP da NF-e. */
@Component
public class NfeSoapGateway {

    private static final Logger log = LoggerFactory.getLogger(NfeSoapGateway.class);

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
        log.debug("NF-e: preparando envelope SOAP para {} (timeoutMillis={})", service.name(),
                properties.getTimeoutMillis());
        try {
            String resposta = transport.enviar(envelopeFactory.criar(service, xmlFiscal), endpoint, service,
                    properties.getTimeoutMillis());
            responseValidator.validar(resposta, service);
            log.debug("NF-e: resposta SOAP válida para {}", service.name());
            return resposta;
        } catch (RuntimeException exception) {
            log.warn("NF-e: falha na comunicação SOAP para {}: {}", service.name(), exception.getMessage());
            throw exception;
        }
    }
}
