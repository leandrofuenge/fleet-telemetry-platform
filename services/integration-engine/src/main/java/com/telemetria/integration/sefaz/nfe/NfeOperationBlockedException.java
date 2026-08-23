package com.telemetria.integration.sefaz.nfe;

/** Indica que uma operação fiscal foi deliberadamente bloqueada por configuração de segurança. */
public class NfeOperationBlockedException extends NfeException {
    public NfeOperationBlockedException(String message) {
        super(message);
    }
}
