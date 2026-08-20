package com.telemetria.integration.sefaz.cte;

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
}