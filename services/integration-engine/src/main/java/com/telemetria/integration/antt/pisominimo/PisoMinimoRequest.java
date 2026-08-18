package com.telemetria.integration.antt.pisominimo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de entrada para a API de cálculo do Piso Mínimo de Frete ANTT.
 */
public record PisoMinimoRequest(
    @JsonProperty("tipo_carga") String tipoCarga,
    @JsonProperty("eixos") int eixos,
    @JsonProperty("distancia") BigDecimal distancia,
    @JsonProperty("retorno_vazio") boolean retornoVazio,
    @JsonProperty("alto_desempenho") boolean altoDesempenho,
    @JsonProperty("composicao_veicular") String composicaoVeicular
) {}