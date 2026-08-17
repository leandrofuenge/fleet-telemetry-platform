package com.telemetria.domain.exception;

public class VeiculoDuplicateException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public VeiculoDuplicateException(String message) {
        super(message);
    }
    
    public VeiculoDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}