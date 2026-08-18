package com.telemetria.integration.senatran.crlve;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SENATRAN - CRLV-e (documento do veiculo)
 */
@Service
public class CrlveService {

    private final CrlveClient client;

    public CrlveService(CrlveClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
