package com.telemetria.integration.sefaz.nfe;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

/** Codec Base32 RFC 4648 delimitado pelo tamanho máximo de XML NF-e configurado. */
@Component
public class NfeBase32Codec {

    private static final char[] ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final NfeProperties properties;

    public NfeBase32Codec(NfeProperties properties) {
        this.properties = properties;
    }

    public String decodificarXml(NfeBase32Request request) {
        if (request == null || request.xmlBase32() == null || request.xmlBase32().isBlank()) {
            throw new NfeException("O campo xmlBase32 é obrigatório.");
        }
        String base32 = request.xmlBase32().replaceAll("[\\s-]", "").replace("=", "").toUpperCase();
        int maxCaracteres = (int) Math.ceil(properties.getMaxXmlBytes() * 8.0 / 5.0) + 7;
        if (base32.length() > maxCaracteres) {
            throw new NfeException("xmlBase32 excede o limite permitido para XML NF-e.");
        }

        try {
            byte[] xmlBytes = decodificar(base32);
            if (xmlBytes.length > properties.getMaxXmlBytes()) {
                throw new NfeException("XML NF-e decodificado excede o limite de "
                        + properties.getMaxXmlBytes() + " bytes.");
            }
            return new String(xmlBytes, StandardCharsets.UTF_8);
        } catch (NfeException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new NfeException("xmlBase32 não contém Base32 RFC 4648 válido.", exception);
        }
    }

    public NfeBase32Response codificarResposta(String xml) {
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        return new NfeBase32Response(codificar(xmlBytes), xmlBytes.length);
    }

    private String codificar(byte[] bytes) {
        StringBuilder resultado = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xFF);
            bits += 8;
            while (bits >= 5) {
                resultado.append(ALFABETO[(buffer >> (bits - 5)) & 0x1F]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            resultado.append(ALFABETO[(buffer << (5 - bits)) & 0x1F]);
        }
        return resultado.toString();
    }

    private byte[] decodificar(String value) {
        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char character : value.toCharArray()) {
            int indice = new String(ALFABETO).indexOf(character);
            if (indice < 0) {
                throw new IllegalArgumentException("Caractere Base32 inválido: " + character);
            }
            buffer = (buffer << 5) | indice;
            bits += 5;
            if (bits >= 8) {
                resultado.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return resultado.toByteArray();
    }
}
