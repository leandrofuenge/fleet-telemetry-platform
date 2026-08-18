package com.telemetria.integration.senatran.renach;

/**
 * Servico de orquestracao: SENATRAN - RENACH (dados do motorista)
 */
public class RenachService {

    private final RenachClient client;

    public RenachService(RenachClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
