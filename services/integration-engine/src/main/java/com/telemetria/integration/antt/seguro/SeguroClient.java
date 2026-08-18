package com.telemetria.integration.antt.seguro;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: ANTT - Seguros obrigatorios do transporte de carga
 */
public interface SeguroClient {

    IntegrationResponse execute(IntegrationRequest request);
}
