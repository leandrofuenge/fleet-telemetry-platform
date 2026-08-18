package com.telemetria.integration.senatran.crlve;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SENATRAN - CRLV-e (documento do veiculo)
 */
public interface CrlveClient {

    IntegrationResponse execute(IntegrationRequest request);
}
