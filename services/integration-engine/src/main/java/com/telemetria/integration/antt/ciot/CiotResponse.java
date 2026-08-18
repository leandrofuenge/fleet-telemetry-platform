package com.telemetria.integration.antt.ciot;

/**
 * Resposta padrão da API de CIOT.
 */
public record CiotResponse(
    boolean sucesso,
    String numeroCiot,    // O código oficial gerado pela ANTT (ex: 123456789012)
    String protocolo,
    String mensagemErro
) {}