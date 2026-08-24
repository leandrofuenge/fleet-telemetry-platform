package com.telemetria.integration.sefaz.cte.infosimples;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapeamento da resposta JSON da API Infosimples para consulta de CT-e.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InfosimplesCteResponse(
    @JsonProperty("code") Integer code,
    @JsonProperty("code_message") String codeMessage,
    @JsonProperty("data") List<CteData> data,
    @JsonProperty("errors") List<String> errors,
    @JsonProperty("site_receipts") List<String> siteReceipts
) {

    /**
     * Helper para verificar se a requisição foi processada com sucesso pela Infosimples (Código 200).
     */
    public boolean isSucesso() {
        return code != null && code == 200 && (errors == null || errors.isEmpty());
    }
}