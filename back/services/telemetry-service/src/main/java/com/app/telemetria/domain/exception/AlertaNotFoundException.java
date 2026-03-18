package com.app.telemetria.domain.exception;

public class AlertaNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public AlertaNotFoundException(Long id) {
        super("Alerta não encontrado com id: " + id);
    }
}