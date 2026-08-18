package com.telemetria.integration.senatran.serpro;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SerproVeiculoRequest(
        @JsonProperty("placa") String placa,
        @JsonProperty("renavam") String renavam
) {
}
