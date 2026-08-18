package com.telemetria.integration.senatran.renavam;

/**
 * Servico de orquestracao: SENATRAN - RENAVAM (dados do veiculo)
 */
public class RenavamService {

    private final RenavamClient client;

    public RenavamService(RenavamClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
