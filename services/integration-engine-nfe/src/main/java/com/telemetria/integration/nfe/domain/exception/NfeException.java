package com.telemetria.integration.nfe.domain.exception;

public class NfeException extends RuntimeException {
    public NfeException(String message) { super(message); }
    public NfeException(String message, Throwable cause) { super(message, cause); }
}
