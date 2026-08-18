package com.telemetria.integration.antt.rntrc;

/**
 * Contrato de integração: ANTT - RNTRC (Reenvio Sob Demanda / SEFAZ)
 */
public interface RntrcClient {

    /**
     * Solicita o reenvio sob demanda de dados do transportador/frota para a SEFAZ.
     *
     * @param placa Placa do veículo (opcional se informado CNPJ)
     * @param cnpj  CNPJ/CPF do transportador (opcional se informada Placa)
     * @return Resposta contendo cStat e xMotivo
     */
    RntrcReenvioResponse solicitarReenvioOnDemand(String placa, String cnpj);
}