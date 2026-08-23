package com.telemetria.integration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Base32UtilsTests {

    @Test
    void codificaEdecodificaConformeRfc4648() {
        assertEquals("MZXW6YTBOI", Base32Utils.encode("foobar"));
        assertEquals("foobar", Base32Utils.decodeToString("MZXW6YTBOI"));
    }

    @Test
    void aceitaFormatoAmigavelParaDigitacao() {
        assertEquals("foobar", Base32Utils.decodeToString("mzxw-6ytb oi==="));
    }

    @Test
    void rejeitaCaracteresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Base32Utils.decode("MZXW6Y!"));
    }
}
