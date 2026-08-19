package com.telemetria.integration.sefaz.cte.retorno;

public record CteStatusResultado(
        int codigo,
        String motivo,
        CteResultadoCategoria categoria,
        String ambiente,
        String uf,
        String versaoAplicacao,
        String dataRecebimento,
        Integer tempoMedio,
        String xmlOriginal) {

    public boolean disponivel() { return codigo == 107 || codigo == 113; }
}
