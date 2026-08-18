package com.telemetria.integration.antt.pisominimo;

/**
 * Contrato de integração: ANTT - Piso Mínimo de Frete
 */
public interface PisoMinimoClient {

    /**
     * Realiza o cálculo do valor mínimo de frete obrigatório para a operação.
     *
     * @param request Parâmetros da viagem (distância, eixos, tipo de carga, retorno)
     * @return Resposta contendo o valor mínimo em R$ ou erro
     */
    PisoMinimoResponse calcularPisoMinimo(PisoMinimoRequest request);
}