package com.app.routing.exception;

/**
 * RN-ROT-001 — Lançada quando o OSRM está indisponível ou retorna resposta inválida.
 * Nunca deve ser engolida silenciosamente; deve sempre propagar para o cliente da API.
 */
public class OsrmIndisponivelException extends RuntimeException {

    public OsrmIndisponivelException(String message) {
        super(message);
    }

    public OsrmIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
