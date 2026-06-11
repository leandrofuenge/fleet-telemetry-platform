package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.GeofenceRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;


@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);

    private final GeofenceRepository geofenceRepository;
    private final TelemetriaRepository telemetriaRepository; // ← Repositório direto
    private final AlertaService alertaService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${geofence.cooldown.minutes:5}")
    private int cooldownMinutes;

    @Autowired
    public GeofenceService(GeofenceRepository geofenceRepository,
                           TelemetriaRepository telemetriaRepository, // ← Mudança
                           AlertaService alertaService,
                           RedisTemplate<String, String> redisTemplate) {
        this.geofenceRepository = geofenceRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.alertaService = alertaService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void verificarGeofences(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculo() == null) {
            return;
        }

        Long tenantId = telemetria.getTenantId();
        Long veiculoId = telemetria.getVeiculoId(); // Usar ID, não UUID
        String veiculoUuid = telemetria.getVeiculo().getUuid();

        List<Geofence> geofences = geofenceRepository.findAtivasPorVeiculo(tenantId, veiculoUuid);
        if (geofences.isEmpty()) {
            return;
        }

        // Buscar última telemetria anterior (evita ciclo de dependência)
        Optional<Telemetria> ultimaAnterior = telemetriaRepository
            .findTopByVeiculoIdAndProcessadoEmBeforeOrderByProcessadoEmDesc(
                veiculoId, telemetria.getProcessadoEm());
        
        Telemetria anterior = ultimaAnterior.orElse(null);

        for (Geofence geofence : geofences) {
            processarGeofence(telemetria, anterior, geofence);
        }
    }

    private void processarGeofence(Telemetria atual, Telemetria anterior, Geofence geofence) {
        boolean estaDentro = pontoEstaDentroGeofence(atual, geofence);
        boolean estavaDentro = anterior != null && pontoEstaDentroGeofence(anterior, geofence);

        TipoTransicao transicao = determinarTransicao(estaDentro, estavaDentro, geofence.getTipoAlerta());
        
        if (transicao != null && !estaEmCooldown(atual, geofence, transicao)) {
            String mensagem = gerarMensagem(transicao, geofence.getNome());
            alertaService.criarAlertaGeofence(atual, geofence, mensagem);
            registrarCooldown(atual, geofence, transicao);
            log.info("Alerta geofence [{}] veículo {}: {}", 
                transicao, atual.getVeiculoId(), mensagem);
        }
    }

    private enum TipoTransicao { ENTRADA, SAIDA }

    private TipoTransicao determinarTransicao(boolean estaDentro, boolean estavaDentro, 
                                               Geofence.TipoAlertaGeofence tipoAlerta) {
        if (tipoAlerta == Geofence.TipoAlertaGeofence.ENTRADA && estaDentro && !estavaDentro) {
            return TipoTransicao.ENTRADA;
        }
        if (tipoAlerta == Geofence.TipoAlertaGeofence.SAIDA && !estaDentro && estavaDentro) {
            return TipoTransicao.SAIDA;
        }
        if (tipoAlerta == Geofence.TipoAlertaGeofence.AMBOS) {
            if (estaDentro && !estavaDentro) return TipoTransicao.ENTRADA;
            if (!estaDentro && estavaDentro) return TipoTransicao.SAIDA;
        }
        return null;
    }

    private String gerarMensagem(TipoTransicao transicao, String geofenceNome) {
        return transicao == TipoTransicao.ENTRADA 
            ? "Veículo entrou na geofence: " + geofenceNome
            : "Veículo saiu da geofence: " + geofenceNome;
    }

    private boolean estaEmCooldown(Telemetria telemetria, Geofence geofence, TipoTransicao transicao) {
        String key = String.format("geofence:cooldown:%d:%d:%s",
            telemetria.getVeiculoId(), geofence.getId(), transicao.name());
        
        // Operação atômica usando SET NX com TTL
        Boolean wasAbsent = redisTemplate.opsForValue()
            .setIfAbsent(key, LocalDateTime.now().toString(), Duration.ofMinutes(cooldownMinutes));
        
        return Boolean.FALSE.equals(wasAbsent); // Se já existia, está em cooldown
    }

    private void registrarCooldown(Telemetria telemetria, Geofence geofence, TipoTransicao transicao) {
        // O cooldown já foi registrado no método estaEmCooldown usando setIfAbsent
        // Este método pode ser removido ou usado para logging adicional
    }

    // ========== CORREÇÃO: Geofence com coordenadas corretas ==========

    private boolean pontoEstaDentroGeofence(Telemetria telemetria, Geofence geofence) {
        double lat = telemetria.getLatitude();
        double lng = telemetria.getLongitude();
        
        if (geofence.getTipo() == Geofence.TipoGeofence.CIRCULO) {
            return pontoEstaDentroCirculo(lat, lng,
                geofence.getLatitudeCentro(), geofence.getLongitudeCentro(),
                geofence.getRaio());
        } else if (geofence.getTipo() == Geofence.TipoGeofence.POLIGONO) {
            return pontoEstaDentroPoligono(lat, lng, geofence.getVertices());
        }
        return false;
    }

    private boolean pontoEstaDentroCirculo(double lat, double lng, double centroLat, double centroLng, double raioKm) {
        return haversine(lat, lng, centroLat, centroLng) <= raioKm;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double RAIO_TERRA_KM = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return RAIO_TERRA_KM * c;
    }

    /**
     * Algoritmo de ponto em polígono (Ray casting) - CORRIGIDO
     * Coordenadas: (latitude, longitude) ou (x, y)
     */
    private boolean pontoEstaDentroPoligono(double lat, double lng, List<Geofence.CoordenadasDto> vertices) {
        if (vertices == null || vertices.size() < 3) {
            log.warn("Polígono com menos de 3 vértices: size={}", vertices != null ? vertices.size() : 0);
            return false;
        }

        boolean dentro = false;
        int n = vertices.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = vertices.get(i).getLat();
            double lngI = vertices.get(i).getLng();
            double latJ = vertices.get(j).getLat();
            double lngJ = vertices.get(j).getLng();

            // Verifica se o ponto está entre as latitudes dos vértices
            boolean entreLatitudes = (lngI > lng) != (lngJ > lng);
            
            if (entreLatitudes) {
                // Calcula a interseção do raio com a aresta
                double intersecaoX = latJ + (latI - latJ) * (lng - lngJ) / (lngI - lngJ);
                if (lat < intersecaoX) {
                    dentro = !dentro;
                }
            }
        }
        
        return dentro;
    }
}