package com.telemetria.domain.exception;

/** Erro permanente no contrato da mensagem; não deve consumir tentativas de retry. */
public class TelemetriaMessageException extends RuntimeException {

    public TelemetriaMessageException(String message) {
        super(message);
    }

    public TelemetriaMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
