package com.telemetria.integration.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Utilitário RFC 4648 para codificação Base32 sem depender de bibliotecas externas. */
public final class Base32Utils {
    private static final char[] ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private Base32Utils() {
    }

    public static String encode(String texto) {
        if (texto == null) return null;
        return encode(texto.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(byte[] bytes) {
        if (bytes == null) return null;
        if (bytes.length == 0) return "";

        StringBuilder resultado = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsNoBuffer = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsNoBuffer += 8;
            while (bitsNoBuffer >= 5) {
                resultado.append(ALFABETO[(buffer >> (bitsNoBuffer - 5)) & 0x1f]);
                bitsNoBuffer -= 5;
            }
        }
        if (bitsNoBuffer > 0) {
            resultado.append(ALFABETO[(buffer << (5 - bitsNoBuffer)) & 0x1f]);
        }
        return resultado.toString();
    }

    public static String decodeToString(String base32) {
        return new String(decode(base32), StandardCharsets.UTF_8);
    }

    public static byte[] decode(String base32) {
        if (base32 == null || base32.isBlank()) return new byte[0];

        String normalizado = base32.replaceAll("[\\s-]", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream resultado = new ByteArrayOutputStream(normalizado.length() * 5 / 8);
        int buffer = 0;
        int bitsNoBuffer = 0;
        for (int indice = 0; indice < normalizado.length(); indice++) {
            int valor = valorDoCaractere(normalizado.charAt(indice));
            if (valor < 0) {
                throw new IllegalArgumentException("Base32 contém caractere inválido na posição " + indice + ".");
            }
            buffer = (buffer << 5) | valor;
            bitsNoBuffer += 5;
            while (bitsNoBuffer >= 8) {
                resultado.write((buffer >> (bitsNoBuffer - 8)) & 0xff);
                bitsNoBuffer -= 8;
            }
        }
        return resultado.toByteArray();
    }

    private static int valorDoCaractere(char caractere) {
        if (caractere >= 'A' && caractere <= 'Z') return caractere - 'A';
        if (caractere >= '2' && caractere <= '7') return caractere - '2' + 26;
        return -1;
    }
}
