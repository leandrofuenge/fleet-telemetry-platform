package com.telemetria.integration.sefaz.cte.infosimples;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParcialParticipante(
    @JsonProperty("cnpj") String cnpj,
    @JsonProperty("nome_razao_social") String nomeRazaoSocial,
    @JsonProperty("ie") String ie,
    @JsonProperty("uf") String uf
) {}
