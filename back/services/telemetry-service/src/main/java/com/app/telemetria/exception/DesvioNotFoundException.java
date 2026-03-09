package com.app.telemetria.exception;

public class DesvioNotFoundException extends RuntimeException {
    
    public DesvioNotFoundException(Long id) {
        super("Desvio de rota não encontrado com id: " + id);
    }
}