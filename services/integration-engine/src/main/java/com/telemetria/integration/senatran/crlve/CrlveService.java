package com.telemetria.integration.senatran.crlve;

/**
 * Servico de orquestracao: SENATRAN - CRLV-e (documento do veiculo)
 */
public class CrlveService {

    private final CrlveClient client;

    public CrlveService(CrlveClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
