package com.telemetria.integration.senatran.serpro.application;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoRequest;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoResponse;

public interface SerproConsultaClient {
    SerproVeiculoResponse consultarVeiculo(SerproVeiculoRequest request);
}
