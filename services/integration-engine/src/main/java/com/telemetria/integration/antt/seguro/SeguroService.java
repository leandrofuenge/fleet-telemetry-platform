package com.telemetria.integration.antt.seguro;

/**
 * Servico de orquestracao: ANTT - Seguros obrigatorios do transporte de carga
 */
public class SeguroService {

    private final SeguroClient client;

    public SeguroService(SeguroClient client) {
        this.client = client;
    }

    // TODO: implementar regras de negocio / validacoes / mapeamento de DTOs
}
