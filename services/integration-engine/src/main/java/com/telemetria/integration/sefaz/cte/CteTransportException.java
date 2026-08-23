package com.telemetria.integration.sefaz.cte;

public class CteTransportException extends RuntimeException {
    public CteTransportException(String message) { super(message); }
    public CteTransportException(String message, Throwable cause) { super(message, cause); }
}
