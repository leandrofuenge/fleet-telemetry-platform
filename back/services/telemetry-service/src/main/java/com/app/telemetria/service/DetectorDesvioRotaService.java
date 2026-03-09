package com.app.telemetria.service;

import com.app.telemetria.entity.*;
import com.app.telemetria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DetectorDesvioRotaService {
    
    private final RotaRepository rotaRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final DesvioRotaRepository desvioRotaRepository;
    private final GeocodingService geocodingService;
    
    // Constantes
    private static final double RAIO_TERRA = 6371000; // metros
    private static final double TOLERANCIA_DESVIO = 50.0; // 50 metros
    
    public DetectorDesvioRotaService(
            RotaRepository rotaRepository,
            TelemetriaRepository telemetriaRepository,
            DesvioRotaRepository desvioRotaRepository,
            GeocodingService geocodingService) {
        this.rotaRepository = rotaRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.desvioRotaRepository = desvioRotaRepository;
        this.geocodingService = geocodingService;
    }
    
    @Transactional
    public void verificarDesviosAtivos() {
        // Busca rotas em andamento
        List<Rota> rotasAtivas = rotaRepository.findByStatus("EM_ANDAMENTO");
        
        for (Rota rota : rotasAtivas) {
            verificarDesvioParaRota(rota);
        }
    }
    
    private void verificarDesvioParaRota(Rota rota) {
        // Busca última telemetria do veículo
    	Telemetria ultimaTelemetria = telemetriaRepository
    		    .findUltimaTelemetriaByVeiculo(rota.getVeiculo())
    		    .orElse(null);
    	
        if (ultimaTelemetria == null) return;
        
        // Calcula distância até a rota planejada
        double distanciaAteRota = calcularDistanciaAteRota(
            ultimaTelemetria.getLatitude(),
            ultimaTelemetria.getLongitude(),
            rota
        );
        
        // Verifica se está em desvio
        if (distanciaAteRota > TOLERANCIA_DESVIO) {
            registrarDesvio(rota, ultimaTelemetria, distanciaAteRota);
        } else {
            verificarRetornoRota(rota, ultimaTelemetria);
        }
    }
    
    private double calcularDistanciaAteRota(double lat, double lng, Rota rota) {
        // Implementação simplificada - distância ponto a linha
        // Implementação de uma biblioteca de geometria como JTS
        
        double distanciaMinima = Double.MAX_VALUE;
        
        // Pontos da rota (e necessario ter a lista de pontos)
        List<PontoRota> pontos = obterPontosRota(rota);
        
        for (int i = 0; i < pontos.size() - 1; i++) {
            PontoRota p1 = pontos.get(i);
            PontoRota p2 = pontos.get(i + 1);
            
            double distancia = distanciaPontoParaSegmento(
                lat, lng,
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude()
            );
            
            distanciaMinima = Math.min(distanciaMinima, distancia);
        }
        
        return distanciaMinima;
    }
    
    private double distanciaPontoParaSegmento(
            double px, double py,
            double x1, double y1,
            double x2, double y2) {
        
        // Algoritmo para distância de ponto a segmento de reta
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = len_sq != 0 ? dot / len_sq : -1;
        
        double xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        double dx = px - xx;
        double dy = py - yy;
        
        return Math.sqrt(dx * dx + dy * dy) * 111320; // Converte graus para metros (aproximado)
    }
    
    private void registrarDesvio(Rota rota, Telemetria telemetria, double distancia) {
        // Verifica se já existe um desvio ativo para esta rota
        DesvioRota desvioAtivo = desvioRotaRepository
            .findByRotaAndResolvidoFalse(rota)
            .orElse(null);
        
        if (desvioAtivo == null) {
            // Cria novo registro de desvio
            DesvioRota desvio = new DesvioRota();
            desvio.setRota(rota);
            desvio.setVeiculo(rota.getVeiculo());
            desvio.setLatitudeDesvio(telemetria.getLatitude());
            desvio.setLongitudeDesvio(telemetria.getLongitude());
            desvio.setDistanciaDesvio(distancia);
            desvio.setDataHoraDesvio(LocalDateTime.now());
            desvio.setResolvido(false);
            
            desvioRotaRepository.save(desvio);
            
            // Dispara notificação
            notificarDesvio(desvio);
        }
    }
    
    private void verificarRetornoRota(Rota rota, Telemetria telemetria) {
        // Verifica se havia um desvio ativo
        DesvioRota desvioAtivo = desvioRotaRepository
            .findByRotaAndResolvidoFalse(rota)
            .orElse(null);
        
        if (desvioAtivo != null) {
            desvioAtivo.setResolvido(true);
            desvioAtivo.setDataHoraRetorno(LocalDateTime.now());
            desvioRotaRepository.save(desvioAtivo);
            
            // Notifica retorno à rota
            notificarRetorno(rota);
        }
    }
    
    private void notificarDesvio(DesvioRota desvio) {
        // Implementar notificação (email, push, websocket, etc)
        String mensagem = String.format(
            "🚨 DESVIO DE ROTA DETECTADO!\n" +
            "Rota: %s\n" +
            "Veículo: %s\n" +
            "Distância: %.2f metros\n" +
            "Local: %.6f, %.6f",
            desvio.getRota().getNome(),
            desvio.getVeiculo().getPlaca(),
            desvio.getDistanciaDesvio(),
            desvio.getLatitudeDesvio(),
            desvio.getLongitudeDesvio()
        );
        
        System.out.println(mensagem);
        // Aqui você pode integrar com websockets, email, etc
    }
    
    private void notificarRetorno(Rota rota) {
        String mensagem = String.format(
            "✅ VEÍCULO RETORNOU À ROTA!\n" +
            "Rota: %s\n" +
            "Veículo: %s",
            rota.getNome(),
            rota.getVeiculo().getPlaca()
        );
        
        System.out.println(mensagem);
    }
    
    private List<PontoRota> obterPontosRota(Rota rota) {
        return geocodingService.obterPontosRota(rota);
    }
}