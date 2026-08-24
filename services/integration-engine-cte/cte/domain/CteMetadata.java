package com.telemetria.integration.sefaz.cte.domain;

/**
 * Metadados extraídos do CT-e durante o processamento inicial.
 *
 * Não representa o CT-e inteiro.
 * Representa somente informações de contexto utilizadas
 * durante o processamento da integração.
 */
public record CteMetadata(
        String chave,
        String numero,
        String serie,
        String modelo,
        String versao
) {
}
