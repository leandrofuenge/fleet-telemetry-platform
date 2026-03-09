package com.app.telemetria.exception;

public class RotaNotFoundException extends RuntimeException {
    
    public RotaNotFoundException(Long id) {
        super("Rota não encontrada com id: " + id);
    }
    
    public RotaNotFoundException(String nome) {
        super("Rota não encontrada com nome: " + nome);
    }
}