package com.telemetria.integration.sefaz.cte.retorno;

public record CteConsultaResultado(
        int codigo,
        String motivo,
        CteResultadoCategoria categoria,
        String protocolo,
        String chaveAcesso,
        String dataRecebimento,
        String xmlOriginal) {

    public boolean autorizado() { return codigo == 100; }
    public boolean cancelado() { return codigo == 101; }
}
