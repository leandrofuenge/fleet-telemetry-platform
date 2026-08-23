package com.telemetria.integration.sefaz.nfe;

/** Resposta SEFAZ codificada em Base64 para transporte JSON seguro. */
public record NfeBase64Response(String xmlBase64, int tamanhoXmlBytes) {
}
