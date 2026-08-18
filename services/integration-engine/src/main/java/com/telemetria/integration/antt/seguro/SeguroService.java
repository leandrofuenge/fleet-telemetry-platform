package com.telemetria.integration.antt.seguro;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: ANTT - Seguros obrigatorios do transporte de carga
 */
@Service
public class SeguroService {

    private final SeguroClient client;

    public SeguroService(SeguroClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
