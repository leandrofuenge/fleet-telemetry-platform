package com.app.telemetria.domain.exception;

public class MotoristaIndisponivelException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public MotoristaIndisponivelException(String message) {
        super(message);
    }
    
    public MotoristaIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}