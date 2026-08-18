package com.telemetria.integration.senatran.serpro;

/**
 * Servico de orquestracao: SERPRO - Consulta Online SENATRAN
 */
public class SerproConsultaService {

    private final SerproConsultaClient client;

    public SerproConsultaService(SerproConsultaClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
