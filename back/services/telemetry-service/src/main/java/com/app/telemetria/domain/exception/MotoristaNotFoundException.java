package com.app.telemetria.domain.exception;

public class MotoristaNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public MotoristaNotFoundException(Long id) {
        super("Motorista não encontrado com id: " + id);
    }
    
    public MotoristaNotFoundException(String cpf) {
        super("Motorista não encontrado com CPF: " + cpf);
    }
}