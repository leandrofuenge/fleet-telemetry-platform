package com.app.telemetria.entity;

// ============================================================
// ARQUIVO: TipoAlerta.java
// ============================================================

public enum TipoAlerta {
    // Velocidade
    EXCESSO_VELOCIDADE,
    VELOCIDADE_BAIXA,
    // Comportamento
    FRENAGEM_BRUSCA,
    ACELERACAO_BRUSCA,
    CURVA_BRUSCA,
    // Rota
    DESVIO_ROTA,
    GEOFENCE_ENTRADA,
    GEOFENCE_SAIDA,
    // Carga
    TEMPERATURA_CARGA_ALTA,
    TEMPERATURA_CARGA_BAIXA,
    UMIDADE_CARGA,
    PORTA_BAU_ABERTA,
    IMPACTO_CARGA,
    // Pneus
    PNEU_PRESSAO_BAIXA,
    PNEU_PRESSAO_ALTA,
    PNEU_TEMPERATURA_ALTA,
    // Segurança
    BOTAO_PANICO,
    COLISAO,
    GPS_SPOOFING,
    ADULTERACAO_ODOMETRO,
    // Motorista / DMS
    FADIGA_DETECTADA,
    DISTRACAO_DETECTADA,
    USO_CELULAR,
    AUSENCIA_CINTO,
    JORNADA_EXCEDIDA,
    PAUSA_OBRIGATORIA,
    // Motor / Manutenção
    TEMPERATURA_MOTOR_ALTA,
    PRESSAO_OLEO_BAIXA,
    BATERIA_FRACA,
    NIVEL_COMBUSTIVEL_BAIXO,
    MANUTENCAO_PENDENTE,
    // Comunicação
    DISPOSITIVO_OFFLINE,
    HEARTBEAT_PERDIDO,
    // Outros
    OUTRO
}
