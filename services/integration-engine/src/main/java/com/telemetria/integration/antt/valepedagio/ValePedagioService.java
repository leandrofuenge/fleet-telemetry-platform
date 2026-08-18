package com.telemetria.integration.antt.valepedagio;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: ANTT - Vale-Pedagio Obrigatorio
 */
@Service
public class ValePedagioService {

    private final ValePedagioClient client;

    public ValePedagioService(ValePedagioClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) {
        return client.execute(request);
    }
}
