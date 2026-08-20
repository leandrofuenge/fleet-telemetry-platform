package com.telemetria.integration.sefaz.cte;

/**
 * Resultado final do processamento de um CT-e.
 */
public record CteProcessResult(

        String chave,

        CteStatus status,

        String codigo,

        String mensagem,

        String protocolo

) {

    public static CteProcessResult autorizado(
            String chave,
            String protocolo,
            String codigo,
            String mensagem
    ) {

        return new CteProcessResult(
                chave,
                CteStatus.AUTORIZADO,
                codigo,
                mensagem,
                protocolo
        );
    }

    public static CteProcessResult rejeitado(
            String chave,
            String codigo,
            String mensagem
    ) {

        return new CteProcessResult(
                chave,
                CteStatus.REJEITADO,
                codigo,
                mensagem,
                null
        );
    }

    public static CteProcessResult erro(
            String chave,
            CteStatus status,
            String mensagem
    ) {

        return new CteProcessResult(
                chave,
                status,
                null,
                mensagem,
                null
        );
    }
}