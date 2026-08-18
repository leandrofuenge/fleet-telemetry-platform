package com.telemetria.integration.senatran.renach;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SENATRAN - RENACH (dados do motorista)
 */
@Service
public class RenachService {

    private final RenachClient client;

    public RenachService(RenachClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
