package com.app.telemetria.enums;

public enum TipoAlerta {
    // Velocidade
    EXCESSO_VELOCIDADE("Excesso de velocidade"),
    VELOCIDADE_BAIXA("Velocidade abaixo do mínimo"),
    
    // Paradas
    PARADA_PROLONGADA("Parada prolongada"),
    PARADA_NAO_PROGRAMADA("Parada não programada"),
    INICIO_MARCHA("Início de marcha"),
    FIM_MARCHA("Fim de marcha"),
    
    // Viagens
    INICIO_VIAGEM("Início de viagem"),
    FIM_VIAGEM("Fim de viagem"),
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

	DISCREPANCIA_LOCALIZACAO("Discrepância entre IP e GPS"),
	LOCALIZACAO_INESPERADA("Localização diferente da rota planejada"),
	PROXY_DETECTADO("Uso de proxy/VPN detectado"),
	ACESSO_EXTERIOR("Acesso de fora do país"),
	LOCALIZACAO_DESCONHECIDA("Localização não pôde ser determinada");
	
	
    private final String descricao;

    TipoAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}