package com.app.telemetria.domain.exception;

public class ViagemNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public ViagemNotFoundException(Long id) {
        super("Viagem não encontrada com id: " + id);
    }
    
    public ViagemNotFoundException(String message) {
        super(message);
    }
}