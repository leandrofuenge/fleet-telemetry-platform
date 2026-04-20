package com.telemetria.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.enums.TipoVia;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.OSRMRoutingService;

/**
 * RN-ROT-002 - Serviço responsável por classificar o tipo de via baseado em coordenadas
 */
@Service
public class ClassificadorViaService {

    private static final Logger log = LoggerFactory.getLogger(ClassificadorViaService.class);

    @Autowired
    private LocationClassifierService locationClassifierService;
    
    @Autowired
    private OSRMRoutingService osrmRoutingService;
    
    /**
     * Classifica o tipo de via baseado nas coordenadas
     * 
     * @param latitude Latitude da posição
     * @param longitude Longitude da posição
     * @return TipoVia correspondente à classificação
     */
    public TipoVia classificar(double latitude, double longitude) {
        log.debug("🔍 Classificando tipo de via para coordenadas: ({}, {})", latitude, longitude);
        
        try {
            // Primeira tentativa: usar LocationClassifierService
            String classificacao = locationClassifierService.classify(latitude, longitude);
            
            if (classificacao != null) {
                TipoVia tipo = TipoVia.fromClassificacao(classificacao);
                log.debug("📍 Via classificada como: {} (tolerância: {}m)", 
                         tipo.getDescricao(), tipo.getToleranciaMetros());
                return tipo;
            }
            
            // Segunda tentativa: consultar OSRM para obter tipo de via
            String highwayType = osrmRoutingService.getHighwayType(latitude, longitude);
            if (highwayType != null) {
                TipoVia tipo = classificarPorHighwayType(highwayType);
                log.debug("📍 Via classificada via OSRM como: {} (highway: {})", 
                         tipo.getDescricao(), highwayType);
                return tipo;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao classificar via: {}", e.getMessage());
        }
        
        // Default: rodovia
        log.debug("📍 Usando classificação padrão: RODOVIA (tolerância: {}m)", 
                 TipoVia.RODOVIA.getToleranciaMetros());
        return TipoVia.RODOVIA;
    }
    
    /**
     * Classifica o tipo de via baseado no highway type do OSRM
     */
    private TipoVia classificarPorHighwayType(String highwayType) {
        if (highwayType == null) {
            return TipoVia.RODOVIA;
        }
        
        String tipo = highwayType.toLowerCase();
        
        // Vias urbanas
        if (tipo.equals("residential") || tipo.equals("living_street") || 
            tipo.equals("pedestrian") || tipo.equals("service") ||
            tipo.equals("tertiary") || tipo.equals("secondary")) {
            return TipoVia.URBANO;
        }
        
        // Rodovias
        if (tipo.equals("motorway") || tipo.equals("trunk") || 
            tipo.equals("primary") || tipo.equals("motorway_link") ||
            tipo.equals("trunk_link")) {
            return TipoVia.RODOVIA;
        }
        
        // Áreas industriais/portuárias (inferir por nome ou contexto)
        if (tipo.equals("industrial") || tipo.equals("commercial")) {
            return TipoVia.PORTO_INDUSTRIAL;
        }
        
        return TipoVia.RODOVIA;
    }
    
    /**
     * Obtém a tolerância para um tipo de via
     */
    public double getTolerancia(TipoVia tipoVia) {
        return tipoVia.getToleranciaMetros();
    }
    
    /**
     * Verifica se uma distância está dentro da tolerância para o tipo de via
     */
    public boolean isDentroTolerancia(TipoVia tipoVia, double distanciaMetros) {
        return distanciaMetros <= tipoVia.getToleranciaMetros();
    }
}