package com.telemetria.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TotpServiceTest {
    private final TotpService service = new TotpService();

    @Test
    void geraCodigoCompativelComVetoresRfc6238() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertEquals("287082", service.gerarCodigo(secret, 1));
    }

    @Test
    void geraSegredoBase32ComTamanhoAdequado() {
        String secret = service.gerarSegredo();
        assertTrue(secret.matches("[A-Z2-7]{32}"));
    }
}
