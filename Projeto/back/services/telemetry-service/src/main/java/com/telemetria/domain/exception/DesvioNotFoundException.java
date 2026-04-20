package com.telemetria.domain.exception;

public class DesvioNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public DesvioNotFoundException(Long id) {
        super("Desvio não encontrado com id: " + id);
    }
    
    public DesvioNotFoundException(String message) {
        super(message);
    }
}