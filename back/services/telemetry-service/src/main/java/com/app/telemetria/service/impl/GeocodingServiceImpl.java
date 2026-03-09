package com.app.telemetria.service.impl;

import com.app.telemetria.entity.PontoRota;
import com.app.telemetria.entity.Rota;
import com.app.telemetria.service.GeocodingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service 
public class GeocodingServiceImpl implements GeocodingService {
    
    @Override
    public List<PontoRota> obterPontosRota(Rota rota) {
        // Versão simplificada - apenas pontos de origem e destino
        List<PontoRota> pontos = new ArrayList<>();
        
        if (rota.getLatitudeOrigem() != null && rota.getLongitudeOrigem() != null) {
            pontos.add(new PontoRota(
                rota.getLatitudeOrigem(), 
                rota.getLongitudeOrigem(), 
                0
            ));
        }
        
        if (rota.getLatitudeDestino() != null && rota.getLongitudeDestino() != null) {
            pontos.add(new PontoRota(
                rota.getLatitudeDestino(), 
                rota.getLongitudeDestino(), 
                1
            ));
        }
        
        return pontos;
    }
    
    @Override
    public double calcularDistanciaAteRota(double latitude, double longitude, List<PontoRota> pontosRota) {
        if (pontosRota == null || pontosRota.isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        double distanciaMinima = Double.MAX_VALUE;
        
        for (PontoRota ponto : pontosRota) {
            double distancia = calcularDistanciaHaversine(
                latitude, longitude,
                ponto.getLatitude(), ponto.getLongitude()
            );
            distanciaMinima = Math.min(distanciaMinima, distancia);
        }
        
        return distanciaMinima;
    }
    
    @Override
    public String obterEndereco(double latitude, double longitude) {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
    
    @Override
    public boolean isPontoNaRota(double latitude, double longitude, List<PontoRota> pontosRota, double tolerancia) {
        double distancia = calcularDistanciaAteRota(latitude, longitude, pontosRota);
        return distancia <= tolerancia;
    }
    
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Raio da Terra em metros
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}