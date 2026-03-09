package com.app.telemetria.service;

import com.app.telemetria.entity.GeocodingCache;
import com.app.telemetria.repository.GeocodingCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class HybridGeocodingService {
    
    // Cache de nível 1 (local - Caffeine)
    private final Map<String, Boolean> memoriaCache;
    
    // Cache de nível 2 (distribuído - Redis)
    private final ValueOperations<String, String> redisCache;
    private final RedisTemplate<String, String> redisTemplate; // Adicionado para GEO
    private static final String GEO_KEY = "geocaching:urbano";
    
    private final GeocodingCacheRepository cacheRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Áreas pré-processadas (em memória)
    private final List<BoundingBox> areasUrbanasPreProcessadas;
    
    @Value("${nominatim.api.url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;
    
    @Value("${app.cache.redis.ttl:604800}")
    private long redisTtl;
    
    private static final long MIN_INTERVALO_MS = 1000;
    private long ultimaConsulta = 0;
    
    public HybridGeocodingService(
            RedisTemplate<String, String> redisTemplate,
            GeocodingCacheRepository cacheRepository) {
        
        this.memoriaCache = new ConcurrentHashMap<>();
        this.redisTemplate = redisTemplate;
        this.redisCache = redisTemplate.opsForValue();
        this.cacheRepository = cacheRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.areasUrbanasPreProcessadas = carregarAreasUrbanasPreProcessadas();
        
        // Inicializar cache GEO com dados pré-processados
        inicializarGeoCache();
    }
    
    /**
     * Inicializa o cache GEO com as áreas urbanas pré-processadas
     */
    private void inicializarGeoCache() {
        for (BoundingBox box : areasUrbanasPreProcessadas) {
            // Adicionar bounding box como pontos representativos
            String id = String.format("bbox:%s:%d", box.tipo, box.populacao);
            
            // Adicionar os 4 cantos do bounding box
            adicionarLocalGeo(id + "_sw", box.minLat, box.minLon);
            adicionarLocalGeo(id + "_se", box.minLat, box.maxLon);
            adicionarLocalGeo(id + "_nw", box.maxLat, box.minLon);
            adicionarLocalGeo(id + "_ne", box.maxLat, box.maxLon);
            
            // Adicionar centro do bounding box
            double centroLat = (box.minLat + box.maxLat) / 2;
            double centroLon = (box.minLon + box.maxLon) / 2;
            adicionarLocalGeo(id + "_center", centroLat, centroLon);
        }
        System.out.println("✅ Cache GEO inicializado com " + areasUrbanasPreProcessadas.size() + " áreas");
    }
    
    /**
     * Método principal - Cache em 3 níveis + GEO
     */
    public boolean verificarAreaUrbana(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return false;
        
        String chaveFormatada = String.format("%.4f,%.4f", latitude, longitude);
        String chaveRedis = "geocoding:" + chaveFormatada;
        
        // 1. CAFFEINE (Nível 1 - mais rápido)
        Boolean cached = memoriaCache.get(chaveFormatada);
        if (cached != null) {
            return cached;
        }
        
        try {
            // 2. REDIS (Nível 2 - distribuído)
            String redisValue = redisCache.get(chaveRedis);
            if (redisValue != null) {
                boolean resultado = Boolean.parseBoolean(redisValue);
                memoriaCache.put(chaveFormatada, resultado);
                return resultado;
            }
            
            // 2.5. REDIS GEO - Busca por proximidade (cache espacial)
            boolean isProximoUrbano = isProximoUrbano(latitude, longitude, 5.0); // raio de 5km
            if (isProximoUrbano) {
                memoriaCache.put(chaveFormatada, true);
                redisCache.set(chaveRedis, "true", redisTtl, TimeUnit.SECONDS);
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao acessar Redis: " + e.getMessage());
        }
        
        // 3. BANCO DE DADOS (Nível 3 - persistente)
        Optional<GeocodingCache> cacheDB = cacheRepository.findProximo(latitude, longitude);
        if (cacheDB.isPresent()) {
            boolean resultado = cacheDB.get().getIsUrbano();
            memoriaCache.put(chaveFormatada, resultado);
            redisCache.set(chaveRedis, String.valueOf(resultado), redisTtl, TimeUnit.SECONDS);
            
            // Também adiciona no GEO cache
            adicionarLocalGeo("cache:" + cacheDB.get().getId(), latitude, longitude);
            
            return resultado;
        }
        
        // 4. ÁREAS PRÉ-PROCESSADAS (fallback rápido)
        Boolean areaPreProcessada = verificarAreaPreProcessada(latitude, longitude);
        if (areaPreProcessada != null) {
            salvarEmTodosCaches(chaveFormatada, chaveRedis, areaPreProcessada);
            adicionarLocalGeo("preprocessado:" + UUID.randomUUID(), latitude, longitude);
            return areaPreProcessada;
        }
        
        // 5. NOMINATIM (preciso, mas lento)
        try {
            Boolean resultado = consultarNominatim(latitude, longitude);
            salvarEmTodosCaches(chaveFormatada, chaveRedis, resultado);
            salvarNoBanco(latitude, longitude, resultado, "nominatim");
            adicionarLocalGeo("nominatim:" + UUID.randomUUID(), latitude, longitude);
            return resultado;
        } catch (Exception e) {
            Boolean fallback = verificarProximidadeCidadesConhecidas(latitude, longitude);
            salvarEmTodosCaches(chaveFormatada, chaveRedis, fallback);
            salvarNoBanco(latitude, longitude, fallback, "fallback");
            adicionarLocalGeo("fallback:" + UUID.randomUUID(), latitude, longitude);
            return fallback;
        }
    }
    
    private void salvarEmTodosCaches(String chaveLocal, String chaveRedis, Boolean valor) {
        memoriaCache.put(chaveLocal, valor);
        redisCache.set(chaveRedis, String.valueOf(valor), redisTtl, TimeUnit.SECONDS);
    }
    
    /**
     * IMPLEMENTAÇÃO REAL do método GEO
     */
    public void adicionarLocalGeo(String localId, double latitude, double longitude) {
        // Point recebe (longitude, latitude) - ATENÇÃO À ORDEM!
        Point point = new Point(longitude, latitude);
        
        redisTemplate.opsForGeo()
            .add(GEO_KEY, point, localId);
        
        // Opcional: expirar após 30 dias (cache longo)
        redisTemplate.expire(GEO_KEY, 30, TimeUnit.DAYS);
    }
    
    /**
     * Verifica se existe área urbana próxima no raio especificado
     */
    public boolean isProximoUrbano(double latitude, double longitude, double raioKm) {
        try {
            Point pontoCentral = new Point(longitude, latitude);
            Distance distance = new Distance(raioKm, Metrics.KILOMETERS);
            
            // Configurar para retornar apenas a contagem (mais rápido)
            Circle circle = new Circle(pontoCentral, distance);
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
                redisTemplate.opsForGeo()
                    .radius(GEO_KEY, circle);
            
            return results != null && !results.getContent().isEmpty();
            
        } catch (Exception e) {
            System.err.println("Erro na busca GEO: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Busca locais urbanos próximos com detalhes
     */
    public List<GeoLocationInfo> buscarLocaisUrbanosProximos(double latitude, double longitude, double raioKm) {
        List<GeoLocationInfo> locais = new ArrayList<>();
        
        try {
            Point pontoCentral = new Point(longitude, latitude);
            Distance distance = new Distance(raioKm, Metrics.KILOMETERS);
            Circle circle = new Circle(pontoCentral, distance);
            
            // Configurar args
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending();
            
            // Usar Circle + Args
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
                redisTemplate.opsForGeo()
                    .radius(GEO_KEY, circle, args);
            
            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                    RedisGeoCommands.GeoLocation<String> location = result.getContent();
                    Point point = location.getPoint();
                    
                    locais.add(new GeoLocationInfo(
                        location.getName(),
                        result.getDistance().getValue(),
                        point.getY(), // latitude
                        point.getX()  // longitude
                    ));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro na busca GEO: " + e.getMessage());
        }
        
        return locais;
    }
    
    /**
     * Classe para retornar informações GEO
     */
    public static class GeoLocationInfo {
        private String id;
        private double distanciaKm;
        private double latitude;
        private double longitude;
        
        public GeoLocationInfo(String id, double distanciaKm, double latitude, double longitude) {
            this.id = id;
            this.distanciaKm = distanciaKm;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        // Getters
        public String getId() { return id; }
        public double getDistanciaKm() { return distanciaKm; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
    
    // ... (mantenha todos os outros métodos existentes: carregarAreasUrbanasPreProcessadas,
    // verificarAreaPreProcessada, consultarNominatim, isTipoUrbano, 
    // verificarProximidadeCidadesConhecidas, salvarNoBanco, calcularDistanciaHaversine)
    
    /**
     * Cache pré-processado (mantido como fallback)
     */
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
            if (lat >= box.minLat && lat <= box.maxLat &&
                lon >= box.minLon && lon <= box.maxLon) {
                return true;
            }
        }
        return null;
    }
    
    private Boolean consultarNominatim(Double lat, Double lon) throws Exception {
        long agora = System.currentTimeMillis();
        if (agora - ultimaConsulta < MIN_INTERVALO_MS) {
            Thread.sleep(MIN_INTERVALO_MS - (agora - ultimaConsulta));
        }
        
        String url = String.format("%s/reverse?lat=%f&lon=%f&format=json&zoom=18",
            nominatimUrl, lat, lon);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "TelemetriaApp/1.0")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        ultimaConsulta = System.currentTimeMillis();
        
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
            "residential", "commercial", "retail", "industrial"
        );
        return tiposUrbanos.contains(type) || tiposUrbanos.contains(category);
    }
    
    private boolean verificarProximidadeCidadesConhecidas(Double lat, Double lon) {
        Map<String, double[]> centrosUrbanos = new HashMap<>();
        centrosUrbanos.put("SaoPaulo", new double[]{-23.5505, -46.6333, 30000.0});
        centrosUrbanos.put("Rio", new double[]{-22.9068, -43.1729, 30000.0});
        centrosUrbanos.put("BH", new double[]{-19.9167, -43.9345, 25000.0});
        centrosUrbanos.put("Brasilia", new double[]{-15.8267, -47.9218, 25000.0});
        centrosUrbanos.put("Salvador", new double[]{-12.9777, -38.5016, 25000.0});
        centrosUrbanos.put("Fortaleza", new double[]{-3.7172, -38.5433, 25000.0});
        centrosUrbanos.put("Curitiba", new double[]{-25.4297, -49.2719, 25000.0});
        centrosUrbanos.put("Manaus", new double[]{-3.1190, -60.0217, 20000.0});
        centrosUrbanos.put("Recife", new double[]{-8.0476, -34.8770, 25000.0});
        centrosUrbanos.put("PortoAlegre", new double[]{-30.0346, -51.2177, 25000.0});
        
        for (Map.Entry<String, double[]> entry : centrosUrbanos.entrySet()) {
            double[] centro = entry.getValue();
            double distancia = calcularDistanciaHaversine(lat, lon, centro[0], centro[1]);
            if (distancia <= centro[2]) {
                return true;
            }
        }
        return false;
    }
    
    private void salvarNoBanco(Double lat, Double lon, Boolean isUrbano, String fonte) {
        GeocodingCache cache = new GeocodingCache();
        cache.setLatitude(lat);
        cache.setLongitude(lon);
        cache.setIsUrbano(isUrbano);
        cache.setDataConsulta(LocalDateTime.now());
        cache.setPrecisaoMetros(100);
        cacheRepository.save(cache);
    }
    
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c * 1000;
    }
    
    private static class BoundingBox {
        double minLat, minLon, maxLat, maxLon;
        String tipo;
        int populacao;
        
        BoundingBox(double minLat, double minLon, double maxLat, double maxLon,
                   String tipo, int populacao) {
            this.minLat = minLat;
            this.minLon = minLon;
            this.maxLat = maxLat;
            this.maxLon = maxLon;
            this.tipo = tipo;
            this.populacao = populacao;
        }
    }
}