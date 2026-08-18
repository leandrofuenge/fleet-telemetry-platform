package com.telemetria.integration.sefaz.evento;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SEFAZ - Eventos fiscais (NF-e / CT-e / MDF-e)
 */
@Service
public class EventoFiscalService {

    private final EventoFiscalClient client;

    public EventoFiscalService(EventoFiscalClient client) {
        this.client = client;
    }

    public IntegrationResponse enviar(IntegrationRequest request) { return client.execute(request); }
}
