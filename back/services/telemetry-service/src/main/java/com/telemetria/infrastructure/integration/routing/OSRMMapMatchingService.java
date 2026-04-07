package com.telemetria.infrastructure.integration.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RN-DEV-001: OSRM Map Matching Service (/match endpoint)
 * Elimina ruido GPS ao encaixar pontos na rota mais provavel
 */
@Service
public class OSRMMapMatchingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${osrm.api.url:https://router.project-osrm.org}")
    private String osrmBaseUrl;

    @Value("${osrm.match.radiuses:15;15;15}")
    private String defaultRadiuses;

    @Value("${osrm.match.gps.precision:15}")
    private int gpsPrecision;

    @Value("${mapmatch.cache.ttl.minutes:5}")
    private int cacheTtlMinutes;

    @Autowired
    public OSRMMapMatchingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * RN-DEV-001: Executa Map Matching para uma sequencia de pontos GPS
     * Retorna coordenadas "snapadas" a rota mais provavel
     * 
     * @param points Lista de pontos no formato [lat, lon, timestamp] ou [lat, lon]
     * @return Lista de coordenadas matchadas ou empty se falhar
     */
    public Optional<List<MatchResult>> matchPoints(List<GPSPoint> points) {
        if (points == null || points.size() < 2) {
            return Optional.empty();
        }

        // Verificar cache primeiro (chave = hash dos pontos)
        String cacheKey = generateCacheKey(points);
        @SuppressWarnings("unchecked")
        List<MatchResult> cached = (List<MatchResult>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            // Montar URL do OSRM /match
            String coordinates = buildCoordinatesParam(points);
            String radiuses = buildRadiusesParam(points.size());
            
            String url = String.format(
                "%s/match/v1/driving/%s?radiuses=%s&geometries=geojson&overview=false&annotations=false",
                osrmBaseUrl, coordinates, radiuses
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Verificar se houve match
            if (root.has("code") && !"Ok".equals(root.get("code").asText())) {
                return Optional.empty();
            }

            // Extrair pontos matchados
            List<MatchResult> results = extractMatchResults(root, points);
            
            // Salvar em cache
            redisTemplate.opsForValue().set(cacheKey, results, Duration.ofMinutes(cacheTtlMinutes));
            
            return Optional.of(results);

        } catch (Exception e) {
            System.err.println("⚠️ Erro no Map Matching OSRM: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * RN-DEV-001: Executa Map Matching para ponto unico (usando /nearest)
     * Fallback quando nao ha historico de pontos
     */
    public Optional<MatchResult> matchSinglePoint(double lat, double lon) {
        List<GPSPoint> points = new ArrayList<>();
        points.add(new GPSPoint(lat, lon, System.currentTimeMillis()));
        
        Optional<List<MatchResult>> results = matchPoints(points);
        if (results.isPresent() && !results.get().isEmpty()) {
            return Optional.of(results.get().get(0));
        }
        return Optional.empty();
    }

    /**
     * RN-DEV-001: Calcula distancia ate a rota usando Map Matching
     * Retorna distancia em metros entre coordenada original e matchada
     */
    public double calcularDistanciaAposMatch(double lat, double lon) {
        Optional<MatchResult> match = matchSinglePoint(lat, lon);
        if (match.isEmpty()) {
            return 0.0; // Nao conseguiu match, assume na rota
        }

        MatchResult result = match.get();
        return calcularDistanciaHaversine(lat, lon, result.matchedLat, result.matchedLon);
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private String buildCoordinatesParam(List<GPSPoint> points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            GPSPoint p = points.get(i);
            sb.append(p.lon).append(",").append(p.lat);
            if (i < points.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    private String buildRadiusesParam(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(gpsPrecision);
            if (i < count - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    private String generateCacheKey(List<GPSPoint> points) {
        // Simplificado: usar primeiro e ultimo ponto + quantidade
        StringBuilder sb = new StringBuilder("mapmatch:");
        if (!points.isEmpty()) {
            GPSPoint first = points.get(0);
            GPSPoint last = points.get(points.size() - 1);
            sb.append(String.format("%.4f,%.4f_%.4f,%.4f_%d", 
                first.lat, first.lon, last.lat, last.lon, points.size()));
        }
        return sb.toString();
    }

    private List<MatchResult> extractMatchResults(JsonNode root, List<GPSPoint> originalPoints) {
        List<MatchResult> results = new ArrayList<>();
        
        JsonNode tracepoints = root.path("tracepoints");
        if (tracepoints.isArray()) {
            for (int i = 0; i < tracepoints.size() && i < originalPoints.size(); i++) {
                JsonNode tp = tracepoints.get(i);
                if (tp != null && !tp.isNull()) {
                    double matchedLon = tp.path("location").get(0).asDouble();
                    double matchedLat = tp.path("location").get(1).asDouble();
                    String name = tp.path("name").asText("Via desconhecida");
                    int matchingsIndex = tp.path("matchings_index").asInt(0);
                    int waypointIndex = tp.path("waypoint_index").asInt(i);
                    
                    GPSPoint original = originalPoints.get(i);
                    double distance = calcularDistanciaHaversine(
                        original.lat, original.lon, matchedLat, matchedLon);
                    
                    results.add(new MatchResult(
                        matchedLat, matchedLon, name, original.lat, original.lon, 
                        distance, matchingsIndex, waypointIndex
                    ));
                }
            }
        }
        
        return results;
    }

    // =========================================
    // Records/DTOs
    // =========================================
    
    public record GPSPoint(double lat, double lon, long timestamp) {
        public GPSPoint(double lat, double lon) {
            this(lat, lon, System.currentTimeMillis());
        }
    }
    
    public record MatchResult(
        double matchedLat,    // Latitude apos match
        double matchedLon,  // Longitude apos match  
        String matchedName, // Nome da via matchada
        double originalLat, // Latitude original
        double originalLon, // Longitude original
        double distanceToMatch, // Distancia em metros ate o match
        int matchingsIndex, // Indice do matching usado
        int waypointIndex   // Indice do waypoint
    ) {
        /**
         * Verifica se o ponto foi matchado com confianca
         * (distancia pequena indica match de qualidade)
         */
        public boolean isGoodMatch(double thresholdMetros) {
            return distanceToMatch <= thresholdMetros;
        }
    }
}
