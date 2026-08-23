package com.telemetria.integration.sefaz.nfe;

/** Falha temporária ou resposta inválida recebida da SEFAZ. */
public class NfeSefazUnavailableException extends NfeException {
    public NfeSefazUnavailableException(String message) {
        super(message);
    }

    public NfeSefazUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
