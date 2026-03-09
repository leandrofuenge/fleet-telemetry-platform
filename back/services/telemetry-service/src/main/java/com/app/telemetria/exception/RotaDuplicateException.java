package com.app.telemetria.exception;

public class RotaDuplicateException extends RuntimeException {
    
    public RotaDuplicateException(String nome) {
        super("Já existe uma rota cadastrada com o nome: " + nome);
    }
}