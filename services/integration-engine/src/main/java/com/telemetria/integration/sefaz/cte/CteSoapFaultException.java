package com.telemetria.integration.sefaz.cte;

public class CteSoapFaultException extends RuntimeException {
    private final CteSoapFault fault;
    public CteSoapFaultException(CteSoapFault fault) {
        super(fault == null ? "SOAP Fault CT-e" : fault.reason());
        this.fault = fault;
    }
    public CteSoapFault fault() { return fault; }
}
