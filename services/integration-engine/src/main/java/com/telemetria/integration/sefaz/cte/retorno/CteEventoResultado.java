package com.telemetria.integration.sefaz.cte.retorno;

public record CteEventoResultado(
        int codigoLote,
        String motivoLote,
        int codigoEvento,
        String motivoEvento,
        CteResultadoCategoria categoria,
        String protocoloEvento,
        String chaveAcesso,
        String tipoEvento,
        Integer sequenciaEvento,
        String dataRegistro,
        String xmlOriginal) {

    public boolean registrado() {
        return categoria == CteResultadoCategoria.SUCESSO
                || categoria == CteResultadoCategoria.CANCELADO;
    }
}
