package com.telemetria.infrastructure.integration.routing;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OSRMSnapToRoadService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${osrm.api.url:https://router.project-osrm.org/nearest/v1/driving/%f,%f}")
    private String osrmUrl;

    @Value("${snap.cache.ttl.hours:24}")
    private int cacheTtlHours;

    @Value("${snap.coordinate.rounding:3}")
    private int roundingDigits;

    private double roundingFactor;

    @Autowired
    public OSRMSnapToRoadService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.roundingFactor = Math.pow(10, roundingDigits);
        System.out.println("🔧 Snap-to-road configurado: rounding=" + roundingDigits + 
                           " casas, cache TTL=" + cacheTtlHours + "h");
    }

    /**
     * RN-TEL-003: Snap-to-road com cache Redis (chave arredondada, TTL 24h)
     */
    public Optional<SnapResult> snapToRoad(double lat, double lon) {
        // 1. Arredondar coordenadas para criar chave de cache
        double roundedLat = Math.round(lat * roundingFactor) / roundingFactor;
        double roundedLng = Math.round(lon * roundingFactor) / roundingFactor;
        String cacheKey = String.format("snap:%." + roundingDigits + "f:%." + roundingDigits + "f", roundedLat, roundedLng);

        // 2. Tentar obter do Redis
        SnapResult cached = (SnapResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            System.out.println("✅ Snap recuperado do cache: " + cacheKey);
            return Optional.of(cached);
        }

        // 3. Se não está em cache, chamar OSRM
        try {
            String url = String.format(osrmUrl, lon, lat); // OSRM usa [lon,lat]
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode waypoints = root.path("waypoints").get(0);
            double snapLat = waypoints.path("location").get(1).asDouble();
            double snapLon = waypoints.path("location").get(0).asDouble();
            String nomeVia = waypoints.path("name").asText("Via não identificada");

            SnapResult result = new SnapResult(snapLat, snapLon, nomeVia);

            // 4. Armazenar no Redis com TTL configurado
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(cacheTtlHours));
            System.out.println("✅ Snap salvo em cache: " + cacheKey);

            return Optional.of(result);

        } catch (Exception e) {
            // Fallback: persistir sem snap (RN-TEL-003)
            System.err.println("⚠️ Erro ao obter snap do OSRM: " + e.getMessage());
            return Optional.empty();
        }
    }

    public record SnapResult(double latSnap, double lngSnap, String nomeVia) {}
}