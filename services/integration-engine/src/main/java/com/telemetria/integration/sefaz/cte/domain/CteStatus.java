package com.telemetria.integration.sefaz.cte.domain;

/**
 * Estados possíveis do processamento de um CT-e.
 */
public enum CteStatus {

    RECEBIDO,

    VALIDADO,

    XSD_VALIDO,

    XSD_INVALIDO,

    REGRAS_VALIDAS,

    REGRAS_INVALIDAS,

    ASSINANDO,

    ASSINADO,

    ENVIANDO_SEFAZ,

    AUTORIZADO,

    REJEITADO,

    ERRO_VALIDACAO,

    ERRO_ASSINATURA,

    ERRO_COMUNICACAO,

    ERRO_PROCESSAMENTO
}
