package com.telemetria.integration.sefaz.nfce;

import org.springframework.stereotype.Service;

import com.telemetria.integration.support.IntegrationRequest;
import com.telemetria.integration.support.IntegrationResponse;

/**
 * Servico de orquestracao: SEFAZ - NFC-e
 */
@Service
public class NfceService {

    private final NfceClient client;

    public NfceService(NfceClient client) {
        this.client = client;
    }

    public IntegrationResponse executar(IntegrationRequest request) { return client.execute(request); }
}
