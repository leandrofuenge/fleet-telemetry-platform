package com.telemetria.integration.sefaz.evento;

/**
 * Servico de orquestracao: SEFAZ - Eventos fiscais (NF-e / CT-e / MDF-e)
 */
public class EventoFiscalService {

    private final EventoFiscalClient client;

    public EventoFiscalService(EventoFiscalClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
