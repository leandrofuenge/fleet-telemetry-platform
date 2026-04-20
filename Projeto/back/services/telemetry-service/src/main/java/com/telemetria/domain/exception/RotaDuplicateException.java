package com.telemetria.domain.exception;

public class RotaDuplicateException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public RotaDuplicateException(String message) {
        super(message);
    }
    
    public RotaDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}