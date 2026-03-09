package com.app.telemetria.exception;

public class VeiculoNotFoundException extends RuntimeException {
    
    public VeiculoNotFoundException(Long id) {
        super("Veículo não encontrado com id: " + id);
    }
    
    public VeiculoNotFoundException(String placa) {
        super("Veículo não encontrado com placa: " + placa);
    }
}