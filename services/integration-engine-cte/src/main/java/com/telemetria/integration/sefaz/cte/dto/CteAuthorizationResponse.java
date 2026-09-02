package com.telemetria.integration.sefaz.cte.dto;

public record CteAuthorizationResponse(
        String chaveAcesso,
        String protocolo,
        String cStat,
        String xMotivo
) {

    public boolean autorizado() {
        return "100".equals(cStat);
    }
}