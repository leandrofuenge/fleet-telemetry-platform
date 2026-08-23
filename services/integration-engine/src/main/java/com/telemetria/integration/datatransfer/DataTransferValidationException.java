package com.telemetria.integration.datatransfer;

public class DataTransferValidationException extends RuntimeException {
    public DataTransferValidationException(String message) {
        super(message);
    }

    public DataTransferValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
