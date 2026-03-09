package com.app.telemetria.exception;

public class VeiculoDuplicateException extends RuntimeException {
    
    public VeiculoDuplicateException(String placa) {
        super("Já existe um veículo cadastrado com a placa: " + placa);
    }
}