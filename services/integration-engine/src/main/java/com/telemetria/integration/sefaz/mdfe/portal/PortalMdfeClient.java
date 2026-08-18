package com.telemetria.integration.sefaz.mdfe.portal;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: Portal Nacional do MDF-e
 */
public interface PortalMdfeClient {

    IntegrationResponse execute(IntegrationRequest request);
}
