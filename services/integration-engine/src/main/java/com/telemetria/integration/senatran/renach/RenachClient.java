package com.telemetria.integration.senatran.renach;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SENATRAN - RENACH (dados do motorista)
 */
public interface RenachClient {

    IntegrationResponse execute(IntegrationRequest request);
}
