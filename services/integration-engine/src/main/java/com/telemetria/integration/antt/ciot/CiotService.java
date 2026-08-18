package com.telemetria.integration.antt.ciot;

/**
 * Servico de orquestracao: ANTT - CIOT (documento obrigatorio da viagem)
 */
public class CiotService {

    private final CiotClient client;

    public CiotService(CiotClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
