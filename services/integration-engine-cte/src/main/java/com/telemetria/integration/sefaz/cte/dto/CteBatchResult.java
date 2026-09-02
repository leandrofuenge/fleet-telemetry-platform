package com.telemetria.integration.sefaz.cte.dto;

import java.util.List;

public record CteBatchResult(
        int total,
        int totalSucessos,
        int totalFalhas,
        List<CteItemResult> itens
) {

    public CteBatchResult {
        itens = List.copyOf(itens);
    }

    public static CteBatchResult vazio() {

        return new CteBatchResult(
                0,
                0,
                0,
                List.of()
        );
    }

    public static CteBatchResult from(
            List<CteItemResult> itens
    ) {

        int sucessos = (int) itens.stream()
                .filter(CteItemResult::sucesso)
                .count();

        int falhas = itens.size() - sucessos;

        return new CteBatchResult(
                itens.size(),
                sucessos,
                falhas,
                itens
        );
    }

    public static CteBatchResult falhaGeral(
            String codigo,
            String mensagem
    ) {

        CteItemResult erro =
                CteItemResult.falha(
                        0,
                        codigo,
                        mensagem
                );

        return new CteBatchResult(
                1,
                0,
                1,
                List.of(erro)
        );
    }
}