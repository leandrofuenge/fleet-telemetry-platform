package com.app.telemetria.domain.exception;

public class VeiculoIndisponivelException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public VeiculoIndisponivelException(String message) {
        super(message);
    }
    
    public VeiculoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}