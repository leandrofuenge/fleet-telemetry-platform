package com.app.telemetria.domain.exception;

public class MotoristaDuplicateException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public MotoristaDuplicateException(String message) {
        super(message);
    }
    
    public MotoristaDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}