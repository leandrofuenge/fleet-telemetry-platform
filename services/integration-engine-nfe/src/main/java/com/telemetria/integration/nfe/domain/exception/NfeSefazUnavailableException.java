package com.telemetria.integration.nfe.domain.exception;

/** Falha temporária ou resposta inválida recebida da SEFAZ. */
public class NfeSefazUnavailableException extends NfeException {
    public NfeSefazUnavailableException(String message) {
        super(message);
    }

    public NfeSefazUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
