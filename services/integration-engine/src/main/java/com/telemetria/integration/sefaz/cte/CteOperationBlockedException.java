package com.telemetria.integration.sefaz.cte;

/** Falha segura para operação fiscal ainda não autorizada operacionalmente. */
public class CteOperationBlockedException extends CteException {

    public CteOperationBlockedException(String message) {
        super(message);
    }

    public CteOperationBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
