package com.telemetria.domain.exception;

public class RotaNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public RotaNotFoundException(Long id) {
        super("Rota não encontrada com id: " + id);
    }
    
    public RotaNotFoundException(String nome) {
        super("Rota não encontrada com nome: " + nome);
    }
}