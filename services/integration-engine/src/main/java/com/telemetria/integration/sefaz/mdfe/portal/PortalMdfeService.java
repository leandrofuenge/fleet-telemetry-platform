package com.telemetria.integration.sefaz.mdfe.portal;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: Portal Nacional do MDF-e
 */
@Service
public class PortalMdfeService {

    private final PortalMdfeClient client;

    public PortalMdfeService(PortalMdfeClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
