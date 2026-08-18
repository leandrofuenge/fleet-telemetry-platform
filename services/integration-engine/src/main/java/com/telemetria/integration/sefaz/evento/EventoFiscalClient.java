package com.telemetria.integration.sefaz.evento;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SEFAZ - Eventos fiscais (NF-e / CT-e / MDF-e)
 */
public interface EventoFiscalClient {

    IntegrationResponse execute(IntegrationRequest request);
}
