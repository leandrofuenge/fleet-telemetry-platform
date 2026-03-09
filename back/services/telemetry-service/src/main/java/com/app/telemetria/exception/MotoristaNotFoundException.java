package com.app.telemetria.exception;

public class MotoristaNotFoundException extends RuntimeException {
    
    public MotoristaNotFoundException(Long id) {
        super("Motorista não encontrado com id: " + id);
    }
    
    public MotoristaNotFoundException(String cpf) {
        super("Motorista não encontrado com CPF: " + cpf);
    }
}