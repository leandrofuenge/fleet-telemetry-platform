package com.telemetria.integration.sefaz.nfe;

/** Corpo JSON para envio de XML NF-e codificado em Base32 RFC 4648. */
public record NfeBase32Request(String xmlBase32) {
}
