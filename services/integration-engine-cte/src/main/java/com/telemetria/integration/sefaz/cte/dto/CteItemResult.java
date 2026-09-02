package com.telemetria.integration.sefaz.cte.dto;

public record CteItemResult(
        int indice,
        boolean sucesso,
        String chaveAcesso,
        String protocolo,
        String cStat,
        String motivo,
        String codigoErro,
        String mensagemErro
) {

    public static CteItemResult sucesso(
            int indice,
            CteProcessingResult resultado
    ) {

        return new CteItemResult(
                indice,
                true,
                resultado.chaveAcesso(),
                resultado.protocolo(),
                resultado.cStat(),
                resultado.motivo(),
                null,
                null
        );
    }

    public static CteItemResult falha(
            int indice,
            String codigoErro,
            String mensagemErro
    ) {

        return new CteItemResult(
                indice,
                false,
                null,
                null,
                null,
                null,
                codigoErro,
                mensagemErro
        );
    }
}