package com.telemetria.infrastructure.security;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class TotpService {
    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String gerarSegredo() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        StringBuilder resultado = new StringBuilder(32);
        int buffer = 0;
        int bits = 0;
        for (byte valor : bytes) {
            buffer = (buffer << 8) | (valor & 0xff);
            bits += 8;
            while (bits >= 5) {
                resultado.append(ALFABETO_BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) resultado.append(ALFABETO_BASE32.charAt((buffer << (5 - bits)) & 31));
        return resultado.toString();
    }

    public boolean validarCodigo(String segredo, String codigo) {
        if (segredo == null || codigo == null || !codigo.matches("\\d{6}")) return false;
        long janelaAtual = System.currentTimeMillis() / 30_000L;
        for (long deslocamento = -1; deslocamento <= 1; deslocamento++) {
            if (gerarCodigo(segredo, janelaAtual + deslocamento).equals(codigo)) return true;
        }
        return false;
    }

    String gerarCodigo(String segredo, long janela) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodificarBase32(segredo), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(janela).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binario % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar código TOTP", e);
        }
    }

    private byte[] decodificarBase32(String valor) {
        String normalizado = valor.replace("=", "").replaceAll("\\s", "").toUpperCase();
        ByteBuffer bytes = ByteBuffer.allocate(normalizado.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char caractere : normalizado.toCharArray()) {
            int indice = ALFABETO_BASE32.indexOf(caractere);
            if (indice < 0) throw new IllegalArgumentException("Segredo TOTP inválido");
            buffer = (buffer << 5) | indice;
            bits += 5;
            if (bits >= 8) {
                bytes.put((byte) ((buffer >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        byte[] resultado = new byte[bytes.position()];
        bytes.flip();
        bytes.get(resultado);
        return resultado;
    }
}
