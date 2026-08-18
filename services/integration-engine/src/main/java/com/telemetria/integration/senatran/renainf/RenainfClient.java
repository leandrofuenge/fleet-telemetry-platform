package com.telemetria.integration.senatran.renainf;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SENATRAN - RENAINF (infracoes / multas)
 */
public interface RenainfClient {

    IntegrationResponse execute(IntegrationRequest request);
}
