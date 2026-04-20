package com.telemetria.domain.enums;

/**
 * RN-ROT-002 - Tipos de via e suas tolerâncias para desvio de rota
 */
public enum TipoVia {
    
    URBANO("Urbano", 80.0, "Área urbana com limite de 80m de tolerância"),
    RODOVIA("Rodovia", 150.0, "Rodovia com limite de 150m de tolerância"),
    PORTO_INDUSTRIAL("Porto/Industrial", 200.0, "Área portuária ou industrial com limite de 200m de tolerância");
    
    private final String descricao;
    private final double toleranciaMetros;
    private final String observacao;
    
    TipoVia(String descricao, double toleranciaMetros, String observacao) {
        this.descricao = descricao;
        this.toleranciaMetros = toleranciaMetros;
        this.observacao = observacao;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public double getToleranciaMetros() {
        return toleranciaMetros;
    }
    
    public String getObservacao() {
        return observacao;
    }
    
    /**
     * Obtém tolerância em quilômetros
     */
    public double getToleranciaKm() {
        return toleranciaMetros / 1000.0;
    }
    
    /**
     * Verifica se a distância está fora da tolerância
     */
    public boolean isForaTolerancia(double distanciaMetros) {
        return distanciaMetros > toleranciaMetros;
    }
    
    /**
     * Retorna o tipo baseado na classificação do geocoding
     */
    public static TipoVia fromClassificacao(String classificacao) {
        if (classificacao == null) {
            return RODOVIA;
        }
        
        switch (classificacao.toUpperCase()) {
            case "AREA_URBANA":
            case "URBANO":
            case "URBAN":
                return URBANO;
            case "PORTO":
            case "INDUSTRIAL":
            case "PORTO_INDUSTRIAL":
                return PORTO_INDUSTRIAL;
            case "RODOVIA":
            case "RODOVIARIA":
            case "HIGHWAY":
            case "ROAD":
            default:
                return RODOVIA;
        }
    }
}