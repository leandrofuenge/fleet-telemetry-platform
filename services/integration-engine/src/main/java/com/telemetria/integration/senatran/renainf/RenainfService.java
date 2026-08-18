package com.telemetria.integration.senatran.renainf;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SENATRAN - RENAINF (infracoes / multas)
 */
@Service
public class RenainfService {

    private final RenainfClient client;

    public RenainfService(RenainfClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
