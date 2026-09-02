package com.telemetria.integration.sefaz.cte.dto;

public record CteProcessingResult(
        String chaveAcesso,
        String protocolo,
        String cStat,
        String motivo
) {
}