package com.telemetria.integration.sefaz.cte.portal;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: Portal Nacional do CT-e
 */
public interface PortalCteClient {

    IntegrationResponse execute(IntegrationRequest request);
}
