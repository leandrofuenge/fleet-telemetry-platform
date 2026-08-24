package com.telemetria.integration.sefaz.cte.exception;

import java.io.Serial;

/**
 * Exceção de regra de negócio disparada quando uma operação fiscal com alteração de estado no CT-e
 * é bloqueada por travas de segurança (ex: flags de operação desativadas, massa de teste
 * não autorizada ou certificado digital A1 expirado/inválido).
 */
public class CteOperationBlockedException extends CteException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CteOperationBlockedException(String message) {
        super(message);
    }

    public CteOperationBlockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CteOperationBlockedException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
    }
}
