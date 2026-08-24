package com.telemetria.domain.exception;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final ErrorCode errorCode;

    // Construtor existente
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // Construtor existente
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    // NOVO CONSTRUTOR - Aceita apenas String (para mensagens simples)
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.VALIDATION_ERROR; // ou um código padrão
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}