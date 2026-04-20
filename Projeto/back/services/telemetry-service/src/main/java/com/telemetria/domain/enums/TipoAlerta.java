package com.telemetria.domain.enums;

public enum TipoAlerta {
    // Velocidade
    EXCESSO_VELOCIDADE("Excesso de velocidade"),
    VELOCIDADE_BAIXA("Velocidade abaixo do mínimo"),

    // Comportamento
    FRENAGEM_BRUSCA("Frenagem brusca"),
    ACELERACAO_BRUSCA("Aceleração brusca"),
    CURVA_BRUSCA("Curva brusca"),

    // Paradas
    PARADA_PROLONGADA("Parada prolongada"),
    PARADA_NAO_PROGRAMADA("Parada não programada"),
    INICIO_MARCHA("Início de marcha"),
    FIM_MARCHA("Fim de marcha"),

    // Viagens
    INICIO_VIAGEM("Início de viagem"),
    FIM_VIAGEM("Fim de viagem"),
    CHEGADA_DESTINO("Chegada ao destino"),
    ATRASO_VIAGEM("Atraso na viagem (≥30 min) - Destinatário notificado"),
    ATRASO_CRITICO_VIAGEM("Atraso crítico na viagem (≥60 min) - Gestor notificado"),
    PREVISAO_CHEGADA("Previsão de chegada"),
    ETA_INDETERMINADO("ETA indeterminado - Veículo parado sem previsão"),

    // Posições GPS
    GPS_SEM_SINAL("GPS sem sinal"),
    ZONA_PERIGO("Entrada em zona de perigo"),
    SAIDA_ZONA_PERIGO("Saída de zona de perigo"),

    // Motorista
    TEMPO_DIRECAO("Tempo de direção excedido"),
    TROCA_MOTORISTA("Troca de motorista"),

    // Combustível
    NIVEL_COMBUSTIVEL_BAIXO("Nível de combustível baixo"),
    ABASTECIMENTO("Abastecimento detectado"),

    // Manutenção
    MANUTENCAO_PROXIMA("Manutenção próxima"),
    MANUTENCAO_ATRASADA("Manutenção atrasada"),

    // Localização
    DISCREPANCIA_LOCALIZACAO("Discrepância entre IP e GPS"),
    LOCALIZACAO_INESPERADA("Localização diferente da rota planejada"),
    PROXY_DETECTADO("Uso de proxy/VPN detectado"),
    ACESSO_EXTERIOR("Acesso de fora do país"),
    LOCALIZACAO_DESCONHECIDA("Localização não pôde ser determinada"),

    // Clima
    CLIMA("Clima / Condições meteorológicas"),
    
    // RN-VEI-002 (Tacógrafo)
    TACOGRAFO_VENCIMENTO("Tacógrafo próximo do vencimento"),
    TACOGRAFO_VENCIDO("Tacógrafo vencido"),
    
    // RN-VEI-003 (Documentos)
    CRLV_VENCIMENTO("CRLV próximo do vencimento"),
    CRLV_VENCIDO("CRLV vencido"),
    SEGURO_VENCIMENTO("Seguro próximo do vencimento"),
    SEGURO_VENCIDO("Seguro vencido"),
    DPVAT_VENCIMENTO("DPVAT próximo do vencimento"),
    DPVAT_VENCIDO("DPVAT vencido"),
    RCF_VENCIMENTO("RCF próximo do vencimento"),
    RCF_VENCIDO("RCF vencido"),
    VISTORIA_VENCIMENTO("Vistoria próxima do vencimento"),
    VISTORIA_VENCIDO("Vistoria vencida"),
    RNTRC_VENCIMENTO("RNTRC próximo do vencimento"),
    RNTRC_VENCIDO("RNTRC vencido"),
    
    // Alertas genéricos para documentos (fallback)
    DOCUMENTO_VENCIMENTO("Documento próximo do vencimento"),
    DOCUMENTO_VENCIDO("Documento vencido"),
    
    // RN-VEI-006 (Inconsistência de odômetro)
    ODOMETRO_INCONSISTENCIA("Inconsistência de odômetro detectada"),
    
    // RN-MOT-002 (CNH)
    CNH_VENCIMENTO("CNH próxima do vencimento"),
    CNH_VENCIDA("CNH vencida"),
    
    // RN-MOT-003 (ASO e MOPP)
    ASO_VENCIMENTO("ASO próximo do vencimento"),
    ASO_VENCIDO("ASO vencido"),
    MOPP_INVALIDO("MOPP inválido para carga perigosa"),
    
    // RN-POS-001 (Veículo sem sinal)
    VEICULO_SEM_SINAL("Veículo sem sinal por mais de 30 minutos"),
    
    // RN-MOT-004 (Score)
    SCORE_BAIXO("Score de comportamento baixo (<600)"),
    SCORE_CRITICO("Score de comportamento crítico (<400)"),
    
    // RN-TEL-002 (Validação de GPS)
    VELOCIDADE_IMPOSIVEL("Velocidade acima do limite consecutiva"),
    SALTO_POSICAO("Salto de posição GPS impossível"),
    HDOP_ALTO("HDOP elevado por tempo prolongado"),
    SATELITES_INSUFICIENTES("Satélites insuficientes em área aberta"),
    
    // Geofence
    GEOFENCE("Entrada/Saída de geofence"),
    
    // Desvio de Rota
    DESVIO_ROTA("Desvio de rota detectado"),
    DESVIO_ROTA_CRITICO("Desvio crítico de rota - Ação necessária"),
    DESVIO_ROTA_REPROVADO("Desvio reprovado pelo gestor - Penalidade aplicada"),
    
    // Dispositivo IoT
    DISPOSITIVO_DESCONECTADO("Dispositivo desconectado"),
    DISPOSITIVO_BATERIA_BAIXA("Bateria do dispositivo baixa"),
    FALHA_COMUNICACAO("Falha de comunicação com dispositivo"),
    
    // Carga
    TEMPERATURA_CARGA_ALTA("Temperatura da carga acima do limite"),
    UMIDADE_CARGA_ALTA("Umidade da carga acima do limite"),
    IMPACTO_CARGA("Impacto na carga detectado"),
    PORTA_BAU_ABERTA("Porta do baú aberta durante viagem"),
    
    // Pneus
    PRESSAO_PNEU_BAIXA("Pressão do pneu baixa"),
    ALERTA_PNEU("Alerta geral de pneu"),
    
    // DMS (Driver Monitoring System)
    FADIGA_DETECTADA("Fadiga do motorista detectada"),
    DISTRACAO_DETECTADA("Distração do motorista detectada"),
    USO_CELULAR_DETECTADO("Uso de celular durante direção"),
    CIGARRO_DETECTADO("Uso de cigarro durante direção"),
    AUSENCIA_CINTO_DMS("Ausência de cinto de segurança (DMS)"),
    
    // Score DMS
    SCORE_DMS_BAIXO("Score DMS baixo - Comportamento de risco"),
    
    // Condições de Pista
    PISTA_MOLHADA("Pista molhada detectada"),
    CONDICAO_PISTA_PERIGOSA("Condição de pista perigosa"),
    
    // Outros
    OUTRO("Tipo de alerta não especificado");

    private final String descricao;

    TipoAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
    
    // Método para buscar por descrição
    public static TipoAlerta fromDescricao(String descricao) {
        for (TipoAlerta tipo : values()) {
            if (tipo.descricao.equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        return OUTRO;
    }
    
    // Método para verificar se é alerta de atraso
    public boolean isAtrasoAlerta() {
        return this == ATRASO_VIAGEM || this == ATRASO_CRITICO_VIAGEM;
    }
    
    // Método para verificar se é alerta de documento vencido
    public boolean isDocumentoVencido() {
        return this == CRLV_VENCIDO || this == SEGURO_VENCIDO || 
               this == DPVAT_VENCIDO || this == RCF_VENCIDO || 
               this == VISTORIA_VENCIDO || this == RNTRC_VENCIDO || 
               this == DOCUMENTO_VENCIDO;
    }
    
    // Método para verificar se é alerta de score
    public boolean isScoreAlerta() {
        return this == SCORE_BAIXO || this == SCORE_CRITICO;
    }
}