package com.telemetria.domain.exception;

public class VeiculoNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L; // ADICIONE
    
    public VeiculoNotFoundException(Long id) {
        super("Veículo não encontrado com id: " + id);
    }
    
    public VeiculoNotFoundException(String placa) {
        super("Veículo não encontrado com placa: " + placa);
    }
}