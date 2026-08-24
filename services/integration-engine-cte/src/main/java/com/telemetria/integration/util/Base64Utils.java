package com.telemetria.integration.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilitário de codificação/decodificação Base64 e compressão GZIP para transferência segura de dados e documentos XML.
 */
public final class Base64Utils {

    private Base64Utils() {
    }

    /**
     * Codifica texto UTF-8 para Base64.
     */
    public static String encode(String text) {
        if (text == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Codifica array de bytes para Base64.
     */
    public static String encode(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodifica string Base64 para texto UTF-8.
     */
    public static String decodeToString(String base64) {
        if (base64 == null || base64.isBlank()) {
            return "";
        }
        byte[] decoded = Base64.getDecoder().decode(base64.trim());
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * Decodifica string Base64 para bytes.
     */
    public static byte[] decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(base64.trim());
    }

    /**
     * Comprime conteúdo (ex: XML) com GZIP e codifica o resultado em Base64.
     * Padrão utilizado em comunicações e lotes de eventos fiscais.
     */
    public static String compressGzipBase64(String content) throws IOException {
        if (content == null || content.isBlank()) {
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Descomprime conteúdo GZIP codificado em Base64 para String UTF-8.
     */
    public static String decompressGzipBase64(String base64Gzip) throws IOException {
        return decompressGzipBase64(base64Gzip, Integer.MAX_VALUE);
    }

    /**
     * Descomprime GZIP com limite de saída para evitar expansão descontrolada
     * de payloads comprimidos.
     */
    public static String decompressGzipBase64(String base64Gzip, int maxOutputBytes) throws IOException {
        if (base64Gzip == null || base64Gzip.isBlank()) {
            return "";
        }
        if (maxOutputBytes < 1) {
            throw new IllegalArgumentException("O limite de saída GZIP deve ser positivo.");
        }
        byte[] compressed = Base64.getDecoder().decode(base64Gzip.trim());
        try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            int total = 0;
            while ((len = gzipIn.read(buffer)) != -1) {
                total += len;
                if (total > maxOutputBytes) {
                    throw new IOException("Conteúdo GZIP excede o limite permitido de " + maxOutputBytes + " bytes.");
                }
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }
}
