package com.app.telemetria.exception;

public class TelemetriaNotFoundException extends RuntimeException {
    
    public TelemetriaNotFoundException(Long id) {
        super("Dados de telemetria não encontrados para o id: " + id);
    }
    
    public TelemetriaNotFoundException(String message) {
        super(message);
    }
}