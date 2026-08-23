package com.telemetria.integration.sefaz.nfe;

/** Resposta SEFAZ codificada em Base32 RFC 4648. */
public record NfeBase32Response(String xmlBase32, int tamanhoXmlBytes) {
}
