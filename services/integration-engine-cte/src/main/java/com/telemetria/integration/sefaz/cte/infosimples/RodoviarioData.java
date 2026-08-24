package com.telemetria.integration.sefaz.cte.infosimples;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RodoviarioData(
    @JsonProperty("rntrc") String rntrc,
    @JsonProperty("ciot") String ciot,
    @JsonProperty("motoristas") List<Object> motoristas,
    @JsonProperty("veiculos") List<Object> veiculos
) {}