package com.telemetria.integration.antt.valepedagio;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: ANTT - Vale-Pedagio Obrigatorio
 */
public interface ValePedagioClient {

    IntegrationResponse execute(IntegrationRequest request);
}
