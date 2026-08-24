package com.telemetria.integration.sefaz.cte.exception;

/**
 * Exceção de domínio utilizada durante o processamento do CT-e.
 */
public class CteException extends RuntimeException {

    public CteException(String message) {
        super(message);
    }

    public CteException(String message, Throwable cause) {
        super(message, cause);
    }

    public CteException(String code, String message) {
        super(code + ": " + message);
    }
}
