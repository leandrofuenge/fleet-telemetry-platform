package com.telemetria.integration.nfe.application.dto;

/** Corpo JSON para envio de XML NF-e codificado em Base64 UTF-8. */
public record NfeBase64Request(String xmlBase64) {
}
