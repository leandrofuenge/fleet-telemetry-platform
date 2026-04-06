package com.app.routing.client;

import com.app.routing.enums.PerfilOsrm;
import com.app.routing.exception.OsrmIndisponivelException;
import com.app.routing.util.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * RN-ROT-001 — Cliente OSRM com:
 *  - Perfil dinâmico (CAMINHAO por padrão, mapeado para string de path)
 *  - Cache Redis: chave MD5(lon_orig,lat_orig,lon_dest,lat_dest,perfil), TTL 7 dias
 *  - Falha explícita se OSRM indisponível — nunca retorna linha reta silenciosa
 */
@Component
public class OsrmClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmClient.class);

    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private static final String CACHE_PREFIX = "osrm:rota:";

    @Value("${osrm.base.url:http://192.168.1.12:5000}")
    private String osrmBaseUrl;

    private final WebClient.Builder builder;
    private final StringRedisTemplate redisTemplate;

    public OsrmClient(WebClient.Builder builder, StringRedisTemplate redisTemplate) {
        this.builder = builder;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Calcula rota via OSRM utilizando o perfil fornecido.
     * Se houver cache Redis válido, retorna imediatamente sem chamar o OSRM.
     *
     * @param origemLat  Latitude de origem
     * @param origemLon  Longitude de origem
     * @param destinoLat Latitude de destino
     * @param destinoLon Longitude de destino
     * @param perfil     Perfil OSRM (ex: CAMINHAO, CARRO). Nunca nulo aqui.
     * @return JSON bruto da resposta OSRM
     * @throws OsrmIndisponivelException se OSRM não estiver acessível (RN-ROT-001)
     */
    public String calcularRota(Double origemLat,
                               Double origemLon,
                               Double destinoLat,
                               Double destinoLon,
                               PerfilOsrm perfil) {

        String chaveCache = gerarChaveCache(origemLat, origemLon, destinoLat, destinoLon, perfil);

        // ── Verificar cache Redis ──────────────────────────────────────────────
        String cached = redisTemplate.opsForValue().get(chaveCache);
        if (cached != null) {
            log.info("✅ [RN-ROT-001] Cache Redis HIT — chave MD5: {}", chaveCache);
            return cached;
        }

        log.info("🔵 [RN-ROT-001] Cache MISS. Chamando OSRM com perfil '{}'", perfil.getPathSegment());

        // ── Construir URL com perfil dinâmico ─────────────────────────────────
        // OSRM espera LONGITUDE,LATITUDE (não latitude,longitude)
        String url = osrmBaseUrl
                + "/route/v1/" + perfil.getPathSegment() + "/"
                + origemLon + "," + origemLat + ";"
                + destinoLon + "," + destinoLat
                + "?overview=full&geometries=geojson";

        log.debug("🔵 URL OSRM: {}", url);

        // ── Chamar OSRM — falha deve ser explícita (RN-ROT-001) ───────────────
        String response;
        try {
            response = builder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("❌ [RN-ROT-001] OSRM retornou erro HTTP {}: {}", e.getStatusCode(), e.getMessage());
            throw new OsrmIndisponivelException(
                    "OSRM retornou erro HTTP " + e.getStatusCode() + ". Rota não pode ser calculada.", e);
        } catch (Exception e) {
            log.error("❌ [RN-ROT-001] OSRM indisponível: {}", e.getMessage());
            throw new OsrmIndisponivelException(
                    "OSRM indisponível. Rota não pode ser calculada sem cálculo real de trajeto.", e);
        }

        if (response == null || response.isBlank()) {
            log.error("❌ [RN-ROT-001] OSRM retornou resposta vazia.");
            throw new OsrmIndisponivelException("OSRM retornou resposta vazia. Rota não pode ser ativada.");
        }

        // ── Salvar no Redis com TTL de 7 dias ─────────────────────────────────
        redisTemplate.opsForValue().set(chaveCache, response, CACHE_TTL);
        log.info("💾 [RN-ROT-001] Rota salva no Redis — chave MD5: {}, TTL: 7 dias", chaveCache);

        return response;
    }

    /**
     * Gera a chave de cache no padrão: osrm:rota:{MD5(coords+perfil)}
     * Atende: "Cache Redis: MD5(coords+perfil), TTL 7 dias" (RN-ROT-001)
     */
    private String gerarChaveCache(Double origemLat, Double origemLon,
                                   Double destinoLat, Double destinoLon,
                                   PerfilOsrm perfil) {
        String raw = origemLon + "," + origemLat + ";"
                   + destinoLon + "," + destinoLat + ";"
                   + perfil.name();
        return CACHE_PREFIX + MD5Util.hash(raw);
    }
}