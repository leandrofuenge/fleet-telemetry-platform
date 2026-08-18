package com.telemetria.integration.antt.ciot;

import java.math.BigDecimal;

/**
 * Payload para solicitação de geração de CIOT.
 */
public record CiotRequest(
    String cnpjContratante,
    String cpfCnpjContratado,
    String cpfMotorista,
    String placaVeiculo,
    String renavam,
    String cepOrigem,
    String cepDestino,
    BigDecimal valorFrete,
    BigDecimal valorPedagio,
    String tipoPagamento // TRANSFERENCIA_BANCARIA, PEF
) {}

