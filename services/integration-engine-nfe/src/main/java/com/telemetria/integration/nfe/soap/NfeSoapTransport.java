package com.telemetria.integration.nfe.soap;

import java.net.URI;
import java.time.Duration;

/** Transporte mTLS para serviços SOAP da NF-e. */
public interface NfeSoapTransport {
    String enviar(String envelopeSoap, URI endpoint, NfeSoapService service, Duration timeout);
}
