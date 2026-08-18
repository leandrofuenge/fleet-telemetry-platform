package com.telemetria.integration.sefaz.cte.infosimples;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TotaisData(
    @JsonProperty("valor_prestacao_servico") BigDecimal valorPrestacaoServico,
    @JsonProperty("valor_a_receber") BigDecimal valorAReceber
) {}
