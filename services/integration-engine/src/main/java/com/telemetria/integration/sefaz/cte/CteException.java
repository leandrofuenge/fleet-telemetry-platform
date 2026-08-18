package com.telemetria.integration.sefaz.cte;

/**
 * Exceção personalizada para capturar e tratar falhas de integração com a SEFAZ (CT-e).
 */
public class CteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CteException(String message) {
        super(message);
    }

    public CteException(String message, Throwable cause) {
        super(message, cause);
    }
}