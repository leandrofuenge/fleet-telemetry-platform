package com.telemetria.integration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class Base64UtilsTests {

    @Test
    void descomprimeConteudoDentroDoLimite() throws IOException {
        String compactado = Base64Utils.compressGzipBase64("documento fiscal");

        assertEquals("documento fiscal", Base64Utils.decompressGzipBase64(compactado, 100));
    }

    @Test
    void bloqueiaDescompressaoAcimaDoLimite() throws IOException {
        String compactado = Base64Utils.compressGzipBase64("x".repeat(10_000));

        assertThrows(IOException.class, () -> Base64Utils.decompressGzipBase64(compactado, 100));
    }
}
