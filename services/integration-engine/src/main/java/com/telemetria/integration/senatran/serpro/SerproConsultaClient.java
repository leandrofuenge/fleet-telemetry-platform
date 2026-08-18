package com.telemetria.integration.senatran.serpro;

public interface SerproConsultaClient {
    SerproVeiculoResponse consultarVeiculo(SerproVeiculoRequest request);
}
