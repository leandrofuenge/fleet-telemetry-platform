package com.telemetria.integration.sefaz.cte.retorno;

public record CteAutorizacaoResultado(
        int codigo,
        String motivo,
        CteResultadoCategoria categoria,
        String protocolo,
        String chaveAcesso,
        String dataRecebimento,
        String digestValue,
        String xmlOriginal) {

    public boolean autorizado() { return codigo == 100; }
}
