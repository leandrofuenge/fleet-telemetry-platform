package com.telemetria.infrastructure.integration.geocoding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.telemetria.domain.entity.GeocodingCache;
import com.telemetria.infrastructure.persistence.GeocodingCacheRepository;

import jakarta.annotation.PostConstruct;

@Service
public class HybridGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(HybridGeocodingService.class);
    private static final String GEO_KEY = "geocaching:urbano";
    private static final long MIN_INTERVALO_MS = 1000;

    // CORREÇÃO: Cache L1 Real com expiração para evitar OutOfMemory de chaves infinitas
    private final Cache<String, Boolean> memoriaCache;

    private final ValueOperations<String, String> redisCache;
    private final RedisTemplate<String, String> redisTemplate;
    private final GeocodingCacheRepository cacheRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<BoundingBox> areasUrbanasPreProcessadas;

    @Value("${nominatim.api.url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;

    @Value("${app.cache.redis.ttl:604800}")
    private long redisTtl;

    private long ultimaConsulta = 0;

    public HybridGeocodingService(
            RedisTemplate<String, String> redisTemplate,
            GeocodingCacheRepository cacheRepository) {

        // Configura o Caffeine para guardar no máximo 10 mil coordenadas e expirar em 1 hora
        this.memoriaCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.redisTemplate = redisTemplate;
        this.redisCache = redisTemplate.opsForValue();
        this.cacheRepository = cacheRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
        this.areasUrbanasPreProcessadas = carregarAreasUrbanasPreProcessadas();
    }

    @PostConstruct
    public void init() {
        try {
            if (redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().ping();
                log.info("✅ Redis conectado com sucesso no HybridGeocodingService.");
                inicializarGeoCache();
            }
        } catch (Exception e) {
            log.error("⚠️ Redis indisponível na inicialização. Operando em modo degradado: {}", e.getMessage());
        }
    }

    private void inicializarGeoCache() {
        try {
            for (BoundingBox box : areasUrbanasPreProcessadas) {
                String id = String.format("bbox:%s:%d", box.tipo, box.populacao);
                adicionarLocalGeo(id + "_sw", box.minLat, box.minLon);
                adicionarLocalGeo(id + "_se", box.minLat, box.maxLon);
                adicionarLocalGeo(id + "_nw", box.maxLat, box.minLon);
                adicionarLocalGeo(id + "_ne", box.maxLat, box.maxLon);

                double centroLat = (box.minLat + box.maxLat) / 2;
                double centroLon = (box.minLon + box.maxLon) / 2;
                adicionarLocalGeo(id + "_center", centroLat, centroLon);
            }
            log.info("✅ Cache GEO inicializado com {} áreas no Redis.", areasUrbanasPreProcessadas.size());
        } catch (Exception e) {
            log.warn("⚠️ Não foi possível semear o cache GEO inicial no Redis: {}", e.getMessage());
        }
    }

    public boolean verificarAreaUrbana(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // Arredonda para 4 casas decimais (precisão ~11m)
        BigDecimal latArred = BigDecimal.valueOf(latitude).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lngArred = BigDecimal.valueOf(longitude).setScale(4, RoundingMode.HALF_UP);
        String chaveFormatada = latArred + "," + lngArred;
        String chaveRedis = "geocoding:" + chaveFormatada;

        // 1. CAMADA L1: CAFFEINE
        Boolean cached = memoriaCache.getIfPresent(chaveFormatada);
        if (cached != null) {
            return cached;
        }

        // 2. CAMADA L2: REDIS (Chave Exata)
        try {
            String redisValue = redisCache.get(chaveRedis);
            if (redisValue != null) {
                boolean resultado = Boolean.parseBoolean(redisValue);
                memoriaCache.put(chaveFormatada, resultado);
                return resultado;
            }
        } catch (Exception e) {
            log.warn("Falha ao acessar chave exata no Redis: {}", e.getMessage());
        }

        // 3. CAMADA L3: BANCO DE DADOS RELACIONAL (PostgreSQL)
        Optional<GeocodingCache> cacheDB = cacheRepository.findByLatArredAndLngArred(latArred, lngArred);
        if (cacheDB.isPresent()) {
            boolean resultado = cacheDB.get().getIsUrbano() != null && cacheDB.get().getIsUrbano();
            memoriaCache.put(chaveFormatada, resultado);
            asyncSincronizarCaches(chaveRedis, "cache:" + cacheDB.get().getId(), latitude, longitude, resultado);
            return resultado;
        }

        // 4. CAMADA L4: ÁREAS PRÉ-PROCESSADAS EM MEMÓRIA
        Boolean areaPreProcessada = verificarAreaPreProcessada(latitude, longitude);
        if (areaPreProcessada != null) {
            persistirResultadoCompleto(latArred, lngArred, latitude, longitude, chaveFormatada, chaveRedis, areaPreProcessada, "preprocessado");
            return areaPreProcessada;
        }

        // 5. CAMADA L5: REDIS GEO (Busca Espacial por Raio de Proximidade)
        // CORREÇÃO: Posicionado após o banco relacional para evitar falsos positivos rurais
        try {
            if (isProximoUrbano(latitude, longitude, 5.0)) {
                persistirResultadoCompleto(latArred, lngArred, latitude, longitude, chaveFormatada, chaveRedis, true, "redis_geo");
                return true;
            }
        } catch (Exception e) {
            log.warn("Falha ao executar busca por raio no Redis GEO: {}", e.getMessage());
        }

        // 6. CAMADA L6: API EXTERNA NOMINATIM (Último caso - Operação Síncrona Bloqueante)
        try {
            Boolean resultado = consultarNominatim(latitude, longitude);
            if (resultado != null) {
                persistirResultadoCompleto(latArred, lngArred, latitude, longitude, chaveFormatada, chaveRedis, resultado, "nominatim");
                return resultado;
            }
        } catch (Exception e) {
            log.error("Erro na consulta síncrona ao Nominatim: {}", e.getMessage());
        }

        // 7. CAMADA L7: FALLBACK FINAL
        Boolean fallback = verificarProximidadeCidadesConhecidas(latitude, longitude);
        persistirResultadoCompleto(latArred, lngArred, latitude, longitude, chaveFormatada, chaveRedis, fallback, "fallback");
        return fallback;
    }

    /**
     * Centraliza e padroniza a gravação e distribuição dos dados em todas as mídias.
     */
    private void persistirResultadoCompleto(BigDecimal latArred, BigDecimal lngArred, double lat, double lng,
                                            String chaveLocal, String chaveRedis, boolean resultado, String origem) {
        memoriaCache.put(chaveLocal, resultado);
        try {
            salvarNoBanco(latArred, lngArred, resultado, origem);
        } catch (Exception e) {
            log.error("Erro ao persistir log de geocoding no banco relacional: {}", e.getMessage());
        }
        asyncSincronizarCaches(chaveRedis, origem + ":" + UUID.randomUUID(), lat, lng, resultado);
    }

    /**
     * Sincroniza o Redis de forma segura sem interceptar a Thread principal em caso de timeout.
     */
    private void asyncSincronizarCaches(String chaveRedis, String geoId, double lat, double lng, boolean resultado) {
        try {
            redisCache.set(chaveRedis, String.valueOf(resultado), redisTtl, TimeUnit.SECONDS);
            if (resultado) {
                adicionarLocalGeo(geoId, lat, lng);
            }
        } catch (Exception e) {
            log.warn("Redis indisponível para sincronização de registros: {}", e.getMessage());
        }
    }

    public void adicionarLocalGeo(String localId, double latitude, double longitude) {
        try {
            Point point = new Point(longitude, latitude);
            redisTemplate.opsForGeo().add(GEO_KEY, point, localId);
            redisTemplate.expire(GEO_KEY, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Erro ao indexar coordenada no Redis GEO: {}", e.getMessage());
        }
    }

    public boolean isProximoUrbano(double latitude, double longitude, double raioKm) {
        try {
            Point pontoCentral = new Point(longitude, latitude);
            Distance distance = new Distance(raioKm, Metrics.KILOMETERS);
            Circle circle = new Circle(pontoCentral, distance);
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(GEO_KEY, circle);
            return results != null && !results.getContent().isEmpty();
        } catch (Exception e) {
            log.warn("Erro na verificação de raio de proximidade no Redis GEO: {}", e.getMessage());
            return false;
        }
    }

    public List<GeoLocationInfo> buscarLocaisUrbanosProximos(double latitude, double longitude, double raioKm) {
        List<GeoLocationInfo> locais = new ArrayList<>();
        try {
            Point pontoCentral = new Point(longitude, latitude);
            Distance distance = new Distance(raioKm, Metrics.KILOMETERS);
            Circle circle = new Circle(pontoCentral, distance);
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeCoordinates()
                    .includeDistance()
                    .sortAscending();

            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(GEO_KEY, circle, args);

            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                    RedisGeoCommands.GeoLocation<String> location = result.getContent();
                    Point point = location.getPoint();
                    if (point != null) {
                        locais.add(new GeoLocationInfo(
                                location.getName(),
                                result.getDistance().getValue(),
                                point.getY(),
                                point.getX()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao listar locais do Redis GEO por raio: {}", e.getMessage());
        }
        return locais;
    }

    // ========== MÉTODOS AUXILIARES E ESTRUTURAS ==========

    private List<BoundingBox> carregarAreasUrbanasPreProcessadas() {
        List<BoundingBox> areas = new ArrayList<>();
        areas.add(new BoundingBox(-23.65, -46.75, -23.45, -46.55, "cidade", 100000));
        areas.add(new BoundingBox(-23.58, -46.70, -23.52, -46.60, "centro", 50000));
        areas.add(new BoundingBox(-23.05, -43.35, -22.75, -43.05, "cidade", 100000));
        areas.add(new BoundingBox(-22.98, -43.25, -22.88, -43.15, "centro", 50000));
        areas.add(new BoundingBox(-20.0, -44.1, -19.7, -43.7, "cidade", 80000));
        return areas;
    }

    private Boolean verificarAreaPreProcessada(Double lat, Double lon) {
        for (BoundingBox box : areasUrbanasPreProcessadas) {
            if (lat >= box.minLat && lat <= box.maxLat && lon >= box.minLon && lon <= box.maxLon) {
                return true;
            }
        }
        return null;
    }

    private Boolean consultarNominatim(Double lat, Double lon) throws Exception {
        synchronized (this) {
            long agora = System.currentTimeMillis();
            if (agora - ultimaConsulta < MIN_INTERVALO_MS) {
                Thread.sleep(MIN_INTERVALO_MS - (agora - ultimaConsulta));
            }
            ultimaConsulta = System.currentTimeMillis();
        }

        String url = String.format("%s/reverse?lat=%f&lon=%f&format=json&zoom=18", nominatimUrl, lat, lon);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "TelemetriaApp/1.0")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String type = root.path("type").asText();
            String category = root.path("category").asText();
            return isTipoUrbano(type, category);
        }
        return null;
    }

    private boolean isTipoUrbano(String type, String category) {
        List<String> tiposUrbanos = Arrays.asList(
                "city", "town", "village", "suburb", "neighbourhood",
                "residential", "commercial", "retail", "industrial");
        return tiposUrbanos.contains(type) || tiposUrbanos.contains(category);
    }

    private boolean verificarProximidadeCidadesConhecidas(Double lat, Double lon) {
        Map<String, double[]> centrosUrbanos = new HashMap<>();
        // Formato: [Latitude, Longitude, Raio Limite em Metros]
        centrosUrbanos.put("SaoPaulo", new double[] { -23.5505, -46.6333, 30000.0 });
        centrosUrbanos.put("Rio", new double[] { -22.9068, -43.1729, 30000.0 });
        centrosUrbanos.put("BH", new double[] { -19.9167, -43.9345, 25000.0 });
        centrosUrbanos.put("Brasilia", new double[] { -15.8267, -47.9218, 25000.0 });
        centrosUrbanos.put("Salvador", new double[] { -12.9777, -38.5016, 25000.0 });
        centrosUrbanos.put("Fortaleza", new double[] { -3.7172, -38.5433, 25000.0 });
        centrosUrbanos.put("Curitiba", new double[] { -25.4297, -49.2719, 25000.0 });
        centrosUrbanos.put("Manaus", new double[] { -3.1190, -60.0217, 20000.0 });
        centrosUrbanos.put("Recife", new double[] { -8.0476, -34.8770, 25000.0 });
        centrosUrbanos.put("PortoAlegre", new double[] { -30.0346, -51.2177, 25000.0 });

        for (Map.Entry<String, double[]> entry : centrosUrbanos.entrySet()) {
            double[] centro = entry.getValue();
            double distanciaMetros = calcularDistanciaHaversine(lat, lon, centro[0], centro[1]);
            if (distanciaMetros <= centro[2]) {
                return true;
            }
        }
        return false;
    }

    private void salvarNoBanco(BigDecimal latArred, BigDecimal lngArred, Boolean isUrbano, String fonte) {
        GeocodingCache cache = GeocodingCache.builder()
                .latArred(latArred)
                .lngArred(lngArred)
                .isUrbano(isUrbano)
                .consultaEm(LocalDateTime.now())
                .precisaoMetros(100)
                .fonte(fonte)
                .expiraEm(LocalDateTime.now().plusDays(30))
                .build();
        cacheRepository.save(cache);
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Raio da Terra em KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Retorno explícito em Metros
    }

    public static class GeoLocationInfo {
        private final String id;
        private final double distanciaKm;
        private final double latitude;
        private final double longitude;

        public GeoLocationInfo(String id, double distanciaKm, double latitude, double longitude) {
            this.id = id;
            this.distanciaKm = distanciaKm;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getId() { return id; }
        public double getDistanciaKm() { return distanciaKm; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    private static class BoundingBox {
        final double minLat, minLon, maxLat, maxLon;
        final String tipo;
        final int populacao;

        BoundingBox(double minLat, double minLon, double maxLat, double maxLon, String tipo, int populacao) {
            this.minLat = minLat;
            this.minLon = minLon;
            this.maxLat = maxLat;
            this.maxLon = maxLon;
            this.tipo = tipo;
            this.populacao = populacao;
        }
    }
}
