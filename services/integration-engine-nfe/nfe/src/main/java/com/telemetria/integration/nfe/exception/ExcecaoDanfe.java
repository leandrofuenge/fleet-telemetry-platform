package com.telemetria.integration.nfe.exception;

public class ExcecaoDanfe extends RuntimeException {

    public ExcecaoDanfe(String message) {
        super(message);
    }

    public ExcecaoDanfe(String message, Throwable cause) {
        super(message, cause);
    }

    public ExcecaoDanfe(Throwable cause) {
        super(cause);
    }
}
