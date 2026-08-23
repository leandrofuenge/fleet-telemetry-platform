package com.telemetria.util;

import java.security.SecureRandom;

/** Gera códigos Base32 RFC 4648 adequados para digitação e QR Code. */
public final class Base32CodeGenerator {
    private static final char[] ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Base32CodeGenerator() {
    }

    public static String gerar(int tamanho) {
        if (tamanho < 8) throw new IllegalArgumentException("O código Base32 deve ter ao menos 8 caracteres.");
        StringBuilder codigo = new StringBuilder(tamanho);
        for (int indice = 0; indice < tamanho; indice++) {
            codigo.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return codigo.toString();
    }

    public static String normalizar(String codigo) {
        if (codigo == null) return "";
        return codigo.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static String formatarParaExibicao(String codigo) {
        String normalizado = normalizar(codigo);
        return normalizado.replaceAll("(.{4})(?!$)", "$1-");
    }
}
