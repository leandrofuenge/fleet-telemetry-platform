package com.telemetria.integration.antt.rntrc;

/**
 * Servico de orquestracao: ANTT - RNTRC (situacao do transportador)
 */
public class RntrcService {

    private final RntrcClient client;

    public RntrcService(RntrcClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
