package com.app.telemetria.exception;

public class AlertaNotFoundException extends RuntimeException {
    
    public AlertaNotFoundException(Long id) {
        super("Alerta não encontrado com id: " + id);
    }
}