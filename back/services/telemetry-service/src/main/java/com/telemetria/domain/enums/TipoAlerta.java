package com.telemetria.domain.enums;

public enum TipoAlerta {
    // Velocidade
    EXCESSO_VELOCIDADE("Excesso de velocidade"),
    VELOCIDADE_BAIXA("Velocidade abaixo do mínimo"),

    // Comportamento
    FRENAGEM_BRUSCA("Frenagem brusca"),

    // Paradas
    PARADA_PROLONGADA("Parada prolongada"),
    PARADA_NAO_PROGRAMADA("Parada não programada"),
    INICIO_MARCHA("Início de marcha"),
    FIM_MARCHA("Fim de marcha"),

    // Viagens
    INICIO_VIAGEM("Início de viagem"),
    FIM_VIAGEM("Fim de viagem"),
    CHEGADA_DESTINO("Chegada ao destino"),
    ATRASO_VIAGEM("Atraso na viagem"),
    PREVISAO_CHEGADA("Previsão de chegada"),

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
    
    // NOVOS ALERTAS PARA RN-VEI-002 (Tacógrafo)
    TACOGRAFO_VENCIMENTO("Tacógrafo próximo do vencimento"),
    TACOGRAFO_VENCIDO("Tacógrafo vencido"),
    
    // NOVOS ALERTAS PARA RN-VEI-003 (Documentos)
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
    
    // NOVO ALERTA PARA RN-VEI-006 (Inconsistência de odômetro)
    ODOMETRO_INCONSISTENCIA("Inconsistência de odômetro detectada"),
    
 // Adicione no enum TipoAlerta, após os outros tipos

 // NOVOS ALERTAS PARA RN-MOT-002 (CNH)
 CNH_VENCIMENTO("CNH próxima do vencimento"),
 CNH_VENCIDA("CNH vencida"),
 

 // NOVOS ALERTAS PARA RN-MOT-003 (ASO e MOPP)
 ASO_VENCIMENTO("ASO próximo do vencimento"),
 ASO_VENCIDO("ASO vencido"),
 MOPP_INVALIDO("MOPP inválido para carga perigosa"),
 
 VEICULO_SEM_SINAL("Veículo sem sinal por mais de 30 minutos"),
 
 // NOVOS ALERTAS PARA RN-MOT-004 (Score)
 SCORE_BAIXO("Score de comportamento baixo"),
 SCORE_CRITICO("Score de comportamento crítico"),
 
 // RN-TEL-002
 VELOCIDADE_IMPOSIVEL("Velocidade acima do limite consecutiva"),
 SALTO_POSICAO("Salto de posição GPS impossível"),
 HDOP_ALTO("HDOP elevado por tempo prolongado"),
 SATELITES_INSUFICIENTES("Satélites insuficientes em área aberta"),
 
 GEOFENCE(""),  // <-- adicionar este

 
    // Outros
    OUTRO("Tipo de alerta não especificado");

    private final String descricao;

    TipoAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}