package com.telemetria.integration.antt.rntrc;

public record RntrcReenvioResponse(
    String cStat,
    String xMotivo
) {
    public boolean isSucesso() {
        return "200".equals(cStat);
    }
}