package com.telemetria.integration.senatran.renainf;

/**
 * Servico de orquestracao: SENATRAN - RENAINF (infracoes / multas)
 */
public class RenainfService {

    private final RenainfClient client;

    public RenainfService(RenainfClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
