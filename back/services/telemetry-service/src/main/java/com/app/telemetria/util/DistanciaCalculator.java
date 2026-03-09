package com.app.telemetria.util;

import org.springframework.stereotype.Component;

@Component
public class DistanciaCalculator {
    
    private static final double RAIO_TERRA = 6371; // km
    
    /**
     * Calcula distância entre dois pontos (Haversine)
     */
    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return RAIO_TERRA * c;
    }
    
    /**
     * Calcula distância de um ponto à rota (segmento de reta entre origem e destino)
     */
    public double calcularDistanciaAteRota(double lat, double lon,
                                            double latOrigem, double lonOrigem,
                                            double latDestino, double lonDestino) {
        
        // Algoritmo de distância de ponto a segmento de reta
        double A = lat - latOrigem;
        double B = lon - lonOrigem;
        double C = latDestino - latOrigem;
        double D = lonDestino - lonOrigem;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = lenSq != 0 ? dot / lenSq : -1;
        
        double xx, yy;
        
        if (param < 0) {
            xx = latOrigem;
            yy = lonOrigem;
        } else if (param > 1) {
            xx = latDestino;
            yy = lonDestino;
        } else {
            xx = latOrigem + param * C;
            yy = lonOrigem + param * D;
        }
        
        double dx = lat - xx;
        double dy = lon - yy;
        
        return Math.sqrt(dx * dx + dy * dy) * 111.32; // Converte graus para km
    }
}