package com.telemetria.integration.sefaz.cte;

import java.net.URI;

/** Transporte mTLS usado pelas operações SOAP CT-e. */
public interface CteSoapTransport {

    String enviar(String soapRequest, URI endpoint, CteSoapService service, int timeoutMillis);
}
