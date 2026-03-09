package com.app.telemetria.service;

import com.app.telemetria.entity.PontoRota;
import com.app.telemetria.entity.Rota;
import java.util.List;

public interface GeocodingService {
    
    /**
     * Obtém os pontos geográficos que formam a rota entre origem e destino
     */
    List<PontoRota> obterPontosRota(Rota rota);
    
    /**
     * Calcula a distância entre um ponto e a rota
     */
    double calcularDistanciaAteRota(double latitude, double longitude, List<PontoRota> pontosRota);
    
    /**
     * Obtém o endereço a partir de coordenadas (reverse geocoding)
     */
    String obterEndereco(double latitude, double longitude);
    
    /**
     * Valida se as coordenadas estão dentro de uma rota
     */
    boolean isPontoNaRota(double latitude, double longitude, List<PontoRota> pontosRota, double tolerancia);
}