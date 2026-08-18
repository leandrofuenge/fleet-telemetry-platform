package com.telemetria.integration.sefaz.nfce;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Contrato de integracao: SEFAZ - NFC-e
 */
public interface NfceClient {

    IntegrationResponse execute(IntegrationRequest request);
}
