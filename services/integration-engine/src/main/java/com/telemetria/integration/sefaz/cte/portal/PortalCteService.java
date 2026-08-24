package com.telemetria.integration.sefaz.cte.portal;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: Portal Nacional do CT-e
 */
@Service
@ConditionalOnBean(PortalCteClient.class)
public class PortalCteService {

    private final PortalCteClient client;

    public PortalCteService(PortalCteClient client) {
        this.client = client;
    }

    public IntegrationResponse consultar(IntegrationRequest request) { return client.execute(request); }
}
