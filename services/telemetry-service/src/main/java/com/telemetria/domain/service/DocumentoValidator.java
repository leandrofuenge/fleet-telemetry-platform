package com.telemetria.domain.service;

public final class DocumentoValidator {
    private DocumentoValidator() {}

    public static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean cnpjValido(String valor) {
        String cnpj = somenteDigitos(valor);
        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) return false;
        int[] pesosPrimeiro = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundo = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digito(cnpj, pesosPrimeiro) == cnpj.charAt(12) - '0'
                && digito(cnpj, pesosSegundo) == cnpj.charAt(13) - '0';
    }

    private static int digito(String valor, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (valor.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
