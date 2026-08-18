package com.telemetria.integration.senatran.renavam;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SENATRAN - RENAVAM (dados do veiculo)
 */
public interface RenavamClient {

    IntegrationResponse execute(IntegrationRequest request);
}
