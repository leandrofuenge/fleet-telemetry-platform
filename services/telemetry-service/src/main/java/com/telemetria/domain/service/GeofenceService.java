package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.GeofenceRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);
    private static final double RAIO_TERRA_KM = 6371.0;

    private final GeofenceRepository geofenceRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final AlertaService alertaService;
    private final StringRedisTemplate redisTemplate;

    @Value("${geofence.cooldown.minutes:5}")
    private long cooldownMinutes;

    public GeofenceService(
            GeofenceRepository geofenceRepository, 
            TelemetriaRepository telemetriaRepository, 
            AlertaService alertaService,
            StringRedisTemplate redisTemplate) {
        this.geofenceRepository = geofenceRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.alertaService = alertaService;
        this.redisTemplate = redisTemplate;
    }

    /** Validação única para criação e alteração de cercas virtuais (RF07). */
    public void validarDefinicao(Geofence geofence) {
        if (geofence == null || geofence.getTenantId() == null
                || geofence.getNome() == null || geofence.getNome().isBlank()
                || geofence.getTipo() == null || geofence.getTipoAlerta() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tenant, nome, tipo e evento da geofence são obrigatórios");
        }

        if (geofence.getTipo() == Geofence.TipoGeofence.CIRCULO) {
            validarCoordenada(geofence.getLatitudeCentro(), geofence.getLongitudeCentro());
            if (geofence.getRaio() == null || geofence.getRaio() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Geofence circular deve possuir raio positivo em quilômetros");
            }
            return;
        }

        List<Geofence.CoordenadasDto> vertices = geofence.getVertices();
        if (vertices == null || vertices.size() < 3) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Geofence poligonal deve possuir ao menos três vértices");
        }
        vertices.forEach(v -> validarCoordenada(v.getLat(), v.getLng()));
    }

    private void validarCoordenada(Double latitude, Double longitude) {
        if (latitude == null || longitude == null || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coordenadas da geofence inválidas");
        }
    }

    /**
     * Orquestrador de verificação de Geofences.
     * REMOVIDO @Transactional: Consultas iniciais não devem prender conexões de escrita do Pool.
     */
    public void verificarGeofences(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculo() == null) {
            return;
        }

        Long tenantId = telemetria.getTenantId();
        Long veiculoId = telemetria.getVeiculoId(); 
        String veiculoUuid = telemetria.getVeiculo().getUuid();

        // 1. Busca cercas virtuais associadas ao veículo
        List<Geofence> geofences = geofenceRepository.findAtivasPorVeiculo(tenantId, veiculoUuid);
        if (geofences.isEmpty()) {
            return;
        }

        // 2. Busca a última telemetria registrada imediatamente antes do pacote atual
        Optional<Telemetria> ultimaAnterior = telemetriaRepository
            .findTopByVeiculoIdAndProcessadoEmBeforeOrderByProcessadoEmDesc(
                veiculoId, telemetria.getProcessadoEm());

        // 3. Processa cada Geofence isoladamente protegendo o loop contra falhas pontuais
        for (Geofence geofence : geofences) {
            try {
                processarGeofenceIsolado(telemetria, ultimaAnterior, geofence);
            } catch (Exception e) {
                log.error("❌ Erro ao processar geofence ID {} para o veículo id {}: {}", 
                        geofence.getId(), veiculoId, e.getMessage(), e);
            }
        }
    }

    /**
     * Processa a lógica geométrica e transição de estado de uma cerca específica.
     * ADICIONADO @Transactional com REQUIRES_NEW para garantir que a gravação do alerta 
     * ocorra em uma transação limpa, rápida e isolada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processarGeofenceIsolado(Telemetria atual, Optional<Telemetria> anteriorOpt, Geofence geofence) {
        
        // Avalia o estado atual do veículo em relação à cerca
        boolean estaDentro = pontoEstaDentroGeofence(atual, geofence);
        
        // Avalia o estado anterior de forma segura (Se for o primeiro ponto da história, assume falso)
        boolean estavaDentro = anteriorOpt.isPresent() && pontoEstaDentroGeofence(anteriorOpt.get(), geofence);

        // Determina se houve ENTRADA, SAÍDA ou NENHUMA alteração baseada na configuração da cerca
        TipoTransicao transicao = determinarTransicao(estaDentro, estavaDentro, geofence.getTipoAlerta());
        
        // Se houve transição válida e ela NÃO violar as regras de Cooldown atômicas do Redis
        if (transicao != null && !estaEmCooldown(atual, geofence, transicao)) {
            
            String mensagem = gerarMensagem(transicao, geofence.getNome());
            
            // Persiste o alerta no banco de dados
            alertaService.criarAlertaGeofence(atual, geofence, mensagem);
            
            log.info("🚨 Alerta geofence [{}] veículo {}: {}", 
                transicao, atual.getVeiculoId(), mensagem);
        }
    }

    private enum TipoTransicao { 
        ENTRADA, SAIDA 
    }

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

    /**
     * Aplica uma trava atômica de Cooldown usando Redis SET NX.
     * Retorna true se o veículo JÁ ESTIVER em período de cooldown para esta transição.
     */
    private boolean estaEmCooldown(Telemetria telemetria, Geofence geofence, TipoTransicao transicao) {
        String key = String.format("geofence:cooldown:%d:%d:%s",
            telemetria.getVeiculoId(), geofence.getId(), transicao.name());
        
        // Operação atômica usando SET NX com TTL (Time-To-Live)
        Boolean wasAbsent = redisTemplate.opsForValue()
            .setIfAbsent(key, LocalDateTime.now().toString(), Duration.ofMinutes(cooldownMinutes));
        
        // Se 'wasAbsent' for falso, significa que a chave já existia (cooldown ativo)
        return Boolean.FALSE.equals(wasAbsent); 
    }

    // ========== CÁLCULOS GEOMÉTRICOS ESPACIAIS ==========

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
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return RAIO_TERRA_KM * c;
    }

    /**
     * Algoritmo de ponto em polígono (Ray-Casting / Crossing Number)
     */
    private boolean pontoEstaDentroPoligono(double lat, double lng, List<Geofence.CoordenadasDto> vertices) {
        if (vertices == null || vertices.size() < 3) {
            log.warn("Polígono com menos de 3 vértices inválido.");
            return false;
        }

        boolean dentro = false;
        int n = vertices.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = vertices.get(i).getLat();
            double lngI = vertices.get(i).getLng();
            double latJ = vertices.get(j).getLat();
            double lngJ = vertices.get(j).getLng();

            // Verifica se a longitude do ponto está entre as longitudes do segmento da aresta
            boolean entreLongitudes = (lngI > lng) != (lngJ > lng);
            
            if (entreLongitudes) {
                // Calcula a interseção do raio horizontal (X) projetado com a aresta do polígono
                double intersecaoX = latJ + (latI - latJ) * (lng - lngJ) / (lngI - lngJ);
                if (lat < intersecaoX) {
                    dentro = !dentro;
                }
            }
        }
        
        return dentro;
    }
}
