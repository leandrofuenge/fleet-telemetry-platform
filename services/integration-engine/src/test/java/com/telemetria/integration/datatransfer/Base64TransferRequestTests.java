package com.telemetria.integration.datatransfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Base64TransferRequestTests {

    @Test
    void separaConfiguracoesDeEntradaESaidaGzip() {
        Base64TransferRequest request = new Base64TransferRequest();
        request.setEntradaCompactadaGzip(true);
        request.setCompactarRespostaGzip(false);

        assertTrue(request.isEntradaCompactadaGzip());
        assertFalse(request.isCompactarRespostaGzip());
    }

    @Test
    void mantemCompatibilidadeComCampoLegado() {
        Base64TransferRequest request = new Base64TransferRequest();
        request.setCompactarGzip(true);

        assertTrue(request.isEntradaCompactadaGzip());
        assertTrue(request.isCompactarRespostaGzip());
    }
}
