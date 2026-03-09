package com.app.telemetria.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CriticalAreaService {
    
    // Configurações de áreas críticas
    private static final List<AreaCritica> AREAS_CRITICAS = Arrays.asList(
        // São Paulo (área metropolitana) - horário de pico
        new AreaCritica(-23.65, -46.75, -23.45, -46.55, "SP", 
            LocalTime.of(17, 0), LocalTime.of(20, 0), 0.3), // 30% da frequência
        
        // Rio de Janeiro - horário comercial
        new AreaCritica(-23.05, -43.35, -22.75, -43.05, "RJ",
            LocalTime.of(8, 0), LocalTime.of(18, 0), 0.5), // 50% da frequência
        
        // Belo Horizonte - horário de almoço
        new AreaCritica(-20.0, -44.1, -19.7, -43.7, "BH",
            LocalTime.of(11, 0), LocalTime.of(14, 0), 0.4) // 40% da frequência
    );
    
    // Estatísticas por veículo
    private final ConcurrentHashMap<Long, EstatisticasVeiculo> estatisticas = new ConcurrentHashMap<>();
    
    /**
     * Verifica se coordenada está em área crítica
     */
    public boolean isAreaCritica(double latitude, double longitude) {
        for (AreaCritica area : AREAS_CRITICAS) {
            if (area.contem(latitude, longitude)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtém fator de redução para uma coordenada
     */
    public double getFatorReducao(double latitude, double longitude) {
        LocalTime agora = LocalTime.now();
        
        for (AreaCritica area : AREAS_CRITICAS) {
            if (area.contem(latitude, longitude) && area.emHorarioCritico(agora)) {
                System.out.println("📍 Área crítica detectada: " + area.nome + 
                    " - Fator de redução: " + (area.fatorReducao * 100) + "%");
                return area.fatorReducao;
            }
        }
        
        return 1.0; // Sem redução
    }
    
    /**
     * Registra processamento de uma mensagem
     */
    public void registrarProcessamento(Long veiculoId, boolean processado) {
        estatisticas.computeIfAbsent(veiculoId, k -> new EstatisticasVeiculo())
            .incrementar(processado);
    }
    
    /**
     * Imprime estatísticas de redução
     */
    public void imprimirEstatisticas() {
        System.out.println("\n📊 ESTATÍSTICAS DE REDUÇÃO");
        System.out.println("============================");
        
        estatisticas.forEach((veiculoId, stats) -> {
            long total = stats.processadas + stats.descartadas;
            double taxaReducao = total > 0 ? 
                100.0 * stats.descartadas / total : 0;
            
            System.out.printf("Veículo %d: %d processadas, %d descartadas, taxa redução: %.1f%%\n",
                veiculoId, stats.processadas, stats.descartadas, taxaReducao);
        });
    }
    
    // ========== CLASSES INTERNAS ==========
    
    /**
     * Representa uma área crítica (bounding box)
     */
    private static class AreaCritica {
        double minLat, maxLat, minLon, maxLon;
        String nome;
        LocalTime inicioCritico, fimCritico;
        double fatorReducao; // 0.0 a 1.0
        
        AreaCritica(double minLat, double minLon, double maxLat, double maxLon,
                   String nome, LocalTime inicio, LocalTime fim, double fator) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.nome = nome;
            this.inicioCritico = inicio;
            this.fimCritico = fim;
            this.fatorReducao = fator;
        }
        
        boolean contem(double lat, double lon) {
            return lat >= minLat && lat <= maxLat &&
                   lon >= minLon && lon <= maxLon;
        }
        
        boolean emHorarioCritico(LocalTime agora) {
            return !agora.isBefore(inicioCritico) && !agora.isAfter(fimCritico);
        }
    }
    
    /**
     * Estatísticas por veículo
     */
    private static class EstatisticasVeiculo {
        long processadas = 0;
        long descartadas = 0;
        
        synchronized void incrementar(boolean processado) {
            if (processado) {
                processadas++;
            } else {
                descartadas++;
            }
        }
    }
}
 
