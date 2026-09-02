package com.telemetria.integration.sefaz.cte.exception;

/**
 * Exceção de domínio utilizada durante o processamento do CT-e.
 */
public class CteException extends RuntimeException {

    private final String code;

    public CteException(String message) {
        super(message);
        this.code = null;
    }

    public CteException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
    }

    public CteException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
