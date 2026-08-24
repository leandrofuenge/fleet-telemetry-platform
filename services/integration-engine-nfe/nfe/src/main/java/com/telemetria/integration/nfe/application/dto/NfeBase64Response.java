package com.telemetria.integration.nfe.application.dto;

/** Resposta SEFAZ codificada em Base64 para transporte JSON seguro. */
public record NfeBase64Response(String xmlBase64, int tamanhoXmlBytes) {
}
