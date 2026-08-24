package com.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentoValidatorTest {
    @Test
    void aceitaCnpjComDigitosVerificadoresValidos() {
        assertTrue(DocumentoValidator.cnpjValido("04.252.011/0001-10"));
    }

    @Test
    void rejeitaCnpjComDigitoVerificadorInvalido() {
        assertFalse(DocumentoValidator.cnpjValido("04.252.011/0001-11"));
    }
}
