package com.telemetria.domain.service;

import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.GeofenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);

    private final GeofenceRepository geofenceRepository;
    private final TelemetriaService telemetriaService;
    private final AlertaService alertaService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${geofence.cooldown.minutes:5}")
    private int cooldownMinutes;

    @Autowired
    public GeofenceService(GeofenceRepository geofenceRepository,
                           TelemetriaService telemetriaService,
                           AlertaService alertaService,
                           RedisTemplate<String, String> redisTemplate) {
        this.geofenceRepository = geofenceRepository;
        this.telemetriaService = telemetriaService;
        this.alertaService = alertaService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Verifica todas as geofences ativas para o veículo e gera alertas de entrada/saída com cooldown.
     */
    @Transactional
    public void verificarGeofences(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculo() == null) {
            return;
        }

        Long tenantId = telemetria.getTenantId();
        String veiculoUuid = telemetria.getVeiculo().getUuid(); // precisa ter getUuid() em Veiculo

        List<Geofence> geofences = geofenceRepository.findAtivasPorVeiculo(tenantId, veiculoUuid);
        if (geofences.isEmpty()) {
            return;
        }

        // Busca a telemetria anterior para saber se o veículo já estava dentro ou fora
        Optional<Telemetria> ultimaAnterior = telemetriaService.buscarUltimaPorVeiculo(telemetria.getVeiculoId());
        Telemetria anterior = ultimaAnterior.orElse(null);

        for (Geofence geofence : geofences) {
            boolean estaDentro = pontoEstaDentroGeofence(telemetria.getLatitude(), telemetria.getLongitude(), geofence);
            boolean estavaDentro = anterior != null && pontoEstaDentroGeofence(anterior.getLatitude(), anterior.getLongitude(), geofence);

            Geofence.TipoAlertaGeofence tipoAlerta = geofence.getTipoAlerta();
            boolean deveGerarAlerta = false;
            String mensagem = null;

            if (tipoAlerta == Geofence.TipoAlertaGeofence.ENTRADA) {
                if (estaDentro && !estavaDentro) {
                    deveGerarAlerta = true;
                    mensagem = "Veículo entrou na geofence: " + geofence.getNome();
                }
            } else if (tipoAlerta == Geofence.TipoAlertaGeofence.SAIDA) {
                if (!estaDentro && estavaDentro) {
                    deveGerarAlerta = true;
                    mensagem = "Veículo saiu da geofence: " + geofence.getNome();
                }
            } else if (tipoAlerta == Geofence.TipoAlertaGeofence.AMBOS) {
                if (estaDentro && !estavaDentro) {
                    deveGerarAlerta = true;
                    mensagem = "Veículo entrou na geofence: " + geofence.getNome();
                } else if (!estaDentro && estavaDentro) {
                    deveGerarAlerta = true;
                    mensagem = "Veículo saiu da geofence: " + geofence.getNome();
                }
            }

            if (deveGerarAlerta) {
                String key = String.format("geofence:cooldown:%d:%d:%s",
                        telemetria.getVeiculoId(), geofence.getId(),
                        (estaDentro ? "ENTRADA" : "SAIDA"));
                Boolean hasKey = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) {
                    log.debug("Cooldown ativo para geofence {} do veículo {}", geofence.getId(), telemetria.getVeiculoId());
                    continue;
                }

                alertaService.criarAlertaGeofence(telemetria, geofence, mensagem);
                redisTemplate.opsForValue().set(key, LocalDateTime.now().toString(), Duration.ofMinutes(cooldownMinutes));
                log.info("Alerta de geofence gerado para veículo {} na geofence {}: {}",
                        telemetria.getVeiculoId(), geofence.getNome(), mensagem);
            }
        }
    }

    // ========== Lógica de ponto dentro de círculo/polígono ==========

    private boolean pontoEstaDentroGeofence(double lat, double lng, Geofence geofence) {
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
        double distancia = haversine(lat, lng, centroLat, centroLng);
        return distancia <= raioKm;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean pontoEstaDentroPoligono(double lat, double lng, List<Geofence.CoordenadasDto> vertices) {
        if (vertices == null || vertices.size() < 3) {
            return false;
        }
        boolean dentro = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = vertices.get(i).getLat();
            double yi = vertices.get(i).getLng();
            double xj = vertices.get(j).getLat();
            double yj = vertices.get(j).getLng();

            boolean intersect = ((yi > lng) != (yj > lng)) &&
                    (lat < (xj - xi) * (lng - yi) / ((yj - yi) != 0 ? (yj - yi) : 1e-9) + xi);
            if (intersect) {
                dentro = !dentro;
            }
        }
        return dentro;
    }
}