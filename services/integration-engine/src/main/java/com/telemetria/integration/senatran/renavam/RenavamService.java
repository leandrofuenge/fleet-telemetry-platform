package com.telemetria.integration.senatran.renavam;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SENATRAN - RENAVAM (dados do veiculo)
 */
@Service
public class RenavamService {

    private final RenavamClient client;

    public RenavamService(RenavamClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
