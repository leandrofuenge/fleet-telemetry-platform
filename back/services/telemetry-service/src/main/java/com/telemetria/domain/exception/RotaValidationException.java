package com.telemetria.domain.exception;

public class RotaValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public RotaValidationException(String message) {
        super(message);
    }
    
    public RotaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}