package com.telemetria.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Base32CodeGeneratorTest {

    @Test
    void geraCodigoComAlfabetoBase32() {
        String codigo = Base32CodeGenerator.gerar(16);

        assertTrue(codigo.matches("[A-Z2-7]{16}"));
        assertEquals(codigo, Base32CodeGenerator.normalizar(Base32CodeGenerator.formatarParaExibicao(codigo)));
    }
}
