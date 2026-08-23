package com.telemetria.integration.sefaz.nfe;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

/** Codifica e decodifica XML NF-e em Base64 respeitando o limite fiscal configurado. */
@Component
public class NfeBase64Codec {

    private final NfeProperties properties;

    public NfeBase64Codec(NfeProperties properties) {
        this.properties = properties;
    }

    public String decodificarXml(NfeBase64Request request) {
        if (request == null || request.xmlBase64() == null || request.xmlBase64().isBlank()) {
            throw new NfeException("O campo xmlBase64 é obrigatório.");
        }
        try {
            byte[] xmlBytes = Base64.getDecoder().decode(request.xmlBase64().trim());
            if (xmlBytes.length > properties.getMaxXmlBytes()) {
                throw new NfeException("XML NF-e decodificado excede o limite de "
                        + properties.getMaxXmlBytes() + " bytes.");
            }
            return new String(xmlBytes, StandardCharsets.UTF_8);
        } catch (NfeException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new NfeException("xmlBase64 não contém Base64 válido.", exception);
        }
    }

    public NfeBase64Response codificarResposta(String xml) {
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        return new NfeBase64Response(Base64.getEncoder().encodeToString(xmlBytes), xmlBytes.length);
    }
}
