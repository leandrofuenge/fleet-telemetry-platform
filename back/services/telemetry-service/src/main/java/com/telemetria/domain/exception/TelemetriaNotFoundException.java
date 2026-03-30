package com.telemetria.domain.exception;

public class TelemetriaNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public TelemetriaNotFoundException(Long id) {
        super("Telemetria não encontrada com id: " + id);
    }
    
    public TelemetriaNotFoundException(String message) {
        super(message);
    }
}