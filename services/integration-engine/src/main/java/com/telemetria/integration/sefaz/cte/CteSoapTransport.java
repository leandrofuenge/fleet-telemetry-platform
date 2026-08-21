package com.telemetria.integration.sefaz.cte;

public interface CteSoapTransport {
    CteSoapResponse send(CteSoapRequest request);
}
