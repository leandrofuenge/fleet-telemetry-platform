package com.telemetria.integration.sefaz.cte.retorno;

import com.telemetria.integration.sefaz.cte.CteException;

public class CteSoapFaultException extends CteException {
    private final String faultCode;

    public CteSoapFaultException(String faultCode, String reason) {
        super("Falha SOAP da SEFAZ [" + faultCode + "]: " + reason);
        this.faultCode = faultCode;
    }

    public String getFaultCode() { return faultCode; }
}
