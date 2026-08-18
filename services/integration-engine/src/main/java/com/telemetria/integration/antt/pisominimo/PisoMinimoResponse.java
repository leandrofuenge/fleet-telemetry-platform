package com.telemetria.integration.antt.pisominimo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapeamento do JSON retornado pela API da Calculadora do Piso Mínimo ANTT.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PisoMinimoResponse(
    // Valores numéricos brutos
    @JsonProperty("valor_total") BigDecimal valorTotal,
    @JsonProperty("valor_ida") BigDecimal valorIda,
    @JsonProperty("valor_retorno_vazio") BigDecimal valorRetornoVazio,
    @JsonProperty("coeficiente_custo_deslocamento") BigDecimal coeficienteCustoDeslocamento,
    @JsonProperty("coeficiente_custo_carga_descarga") BigDecimal coeficienteCustoCargaDescarga,
    @JsonProperty("distancia") BigDecimal distancia,
    @JsonProperty("operacao_transporte") String operacaoTransporte,

    // Valores formatados / normalizados
    @JsonProperty("normalizado_valor_total") String normalizadoValorTotal,
    @JsonProperty("normalizado_valor_ida") String normalizadoValorIda,
    @JsonProperty("normalizado_valor_retorno_vazio") String normalizadoValorRetornoVazio,
    @JsonProperty("normalizado_coeficiente_custo_deslocamento") String normalizadoCoeficienteCustoDeslocamento,
    @JsonProperty("normalizado_coeficiente_custo_carga_descarga") String normalizadoCoeficienteCustoCargaDescarga,
    @JsonProperty("normalizado_distancia") String normalizadoDistancia,

    // Controle interno de erro/sucesso da integração
    Boolean sucesso,
    String mensagemErro
) {
    public static PisoMinimoResponse erro(String msg) {
        return new PisoMinimoResponse(
            null, null, null, null, null, null, null,
            null, null, null, null, null, null,
            false, msg
        );
    }

    public boolean isSucesso() {
        return Boolean.TRUE.equals(sucesso);
    }
}