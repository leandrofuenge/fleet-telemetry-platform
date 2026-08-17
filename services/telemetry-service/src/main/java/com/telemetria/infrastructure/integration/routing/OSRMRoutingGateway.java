package com.telemetria.infrastructure.integration.routing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RN-ROT-001 — Integração com OSRM para obtenção de trajeto real.
 * RN-ROT-002 — Obtenção do tipo de via para classificação de tolerância.
 */
@Service
public class OSRMRoutingGateway {

    private static final Logger log =
            LoggerFactory.getLogger(OSRMRoutingGateway.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate redis;

    @Value("${osrm.routing.base-url:http://192.168.1.12:5000}")
    private String osrmBaseUrl;

    @Value("${osrm.routing.profile:caminhao}")
    private String perfilPadrao;

    public OSRMRoutingGateway(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * RN-ROT-001
     * Obtém o trajeto real entre origem e destino utilizando OSRM.
     */
    public List<double[]> obterTrajeto(
            double latitudeOrigem,
            double longitudeOrigem,
            double latitudeDestino,
            double longitudeDestino) {

        String cacheKey = chaveRota(latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino, perfilPadrao);
        String respostaJson = redis.opsForValue().get(cacheKey);
        String url = String.format(
                "%s/route/v1/%s/%f,%f;%f,%f?overview=full&geometries=geojson",
                osrmBaseUrl, perfilPadrao,
                longitudeOrigem,
                latitudeOrigem,
                longitudeDestino,
                latitudeDestino);

        log.info("🔵 [RN-ROT-001] Consultando rota OSRM: {}", url);

        if (respostaJson == null) {
            try {
                respostaJson = restTemplate.getForObject(url, String.class);
                if (respostaJson != null && !respostaJson.isBlank()) {
                    redis.opsForValue().set(cacheKey, respostaJson, Duration.ofDays(7));
                }
            } catch (RestClientException e) {
                log.error("❌ [RN-ROT-001] OSRM indisponível: {}", e.getMessage());
                throw new RuntimeException("[RN-ROT-001] OSRM indisponível. Não é possível calcular o trajeto.", e);
            }
        }

        if (respostaJson == null || respostaJson.isBlank()) {
            throw new RuntimeException(
                    "[RN-ROT-001] OSRM retornou resposta vazia.");
        }

        try {

            JsonNode root = objectMapper.readTree(respostaJson);

            List<double[]> coordenadasTrajeto = new ArrayList<>();

            JsonNode rotas = root.path("routes");

            if (rotas.isEmpty()) {
                log.warn("⚠️ Nenhuma rota encontrada pelo OSRM.");
                return coordenadasTrajeto;
            }

            JsonNode geometria = rotas.get(0).path("geometry");
            JsonNode coordenadas = geometria.path("coordinates");

            for (JsonNode coordenada : coordenadas) {

                double longitude = coordenada.get(0).asDouble();
                double latitude = coordenada.get(1).asDouble();

                coordenadasTrajeto.add(
                        new double[] { latitude, longitude });
            }

            log.info(
                    "✅ [RN-ROT-001] Trajeto obtido com {} pontos.",
                    coordenadasTrajeto.size());

            return coordenadasTrajeto;

        } catch (Exception e) {

            log.error(
                    "❌ Erro ao interpretar resposta do OSRM: {}",
                    e.getMessage());

            throw new RuntimeException(
                    "Erro ao interpretar resposta do OSRM.",
                    e);
        }
    }

    private String chaveRota(double latOrigem, double lngOrigem, double latDestino, double lngDestino, String perfil) {
        String valor = latOrigem + "," + lngOrigem + ";" + latDestino + "," + lngDestino + ":" + perfil;
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) hash.append(String.format("%02x", b));
            return "osrm:route:" + hash;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar a chave de cache da rota", e);
        }
    }

    /**
     * RN-ROT-002
     * Obtém o tipo da via mais próxima.
     */
    public String obterTipoVia(
            double latitude,
            double longitude) {

        String url = String.format(
                "http://192.168.1.12:5000/nearest/v1/caminhao/%f,%f?number=1",
                longitude,
                latitude);

        log.debug(
                "🔍 [RN-ROT-002] Consultando tipo de via: {}",
                url);

        try {

            String respostaJson =
                    restTemplate.getForObject(url, String.class);

            if (respostaJson == null || respostaJson.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(respostaJson);

            JsonNode vias = root.path("ways");

            if (vias.isEmpty()) {
                return null;
            }

            JsonNode viaMaisProxima = vias.get(0);

            String tipoVia =
                    viaMaisProxima.path("tags")
                                  .path("highway")
                                  .asText(null);

            if (tipoVia != null && !tipoVia.isBlank()) {
                return tipoVia;
            }

            String classeVia =
                    viaMaisProxima.path("tags")
                                  .path("class")
                                  .asText(null);

            return classeVia;

        } catch (Exception e) {

            log.warn(
                    "⚠️ [RN-ROT-002] Erro ao obter tipo da via: {}",
                    e.getMessage());

            return null;
        }
    }

    /**
     * RN-ROT-002
     * Obtém os metadados da via mais próxima.
     */
    public JsonNode obterInformacoesVia(
            double latitude,
            double longitude) {

        String url = String.format(
                "http://192.168.1.12:5000/nearest/v1/caminhao/%f,%f?number=1",
                longitude,
                latitude);

        try {

            String respostaJson =
                    restTemplate.getForObject(url, String.class);

            if (respostaJson == null || respostaJson.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(respostaJson);

            JsonNode vias = root.path("ways");

            if (vias.isEmpty()) {
                return null;
            }

            return vias.get(0);

        } catch (Exception e) {

            log.warn(
                    "⚠️ Erro ao obter informações da via: {}",
                    e.getMessage());

            return null;
        }
    }

    /**
     * RN-ROT-002
     * Verifica se o ponto está localizado em rodovia.
     */
    public boolean estaEmRodovia(
            double latitude,
            double longitude) {

        String tipoVia =
                obterTipoVia(latitude, longitude);

        if (tipoVia == null) {
            return false;
        }

        tipoVia = tipoVia.toLowerCase();

        return tipoVia.equals("motorway")
                || tipoVia.equals("trunk")
                || tipoVia.equals("primary")
                || tipoVia.equals("motorway_link")
                || tipoVia.equals("trunk_link");
    }

    /**
     * RN-ROT-002
     * Verifica se o ponto está localizado em área urbana.
     */
    public boolean estaEmAreaUrbana(
            double latitude,
            double longitude) {

        String tipoVia =
                obterTipoVia(latitude, longitude);

        if (tipoVia == null) {
            return false;
        }

        tipoVia = tipoVia.toLowerCase();

        return tipoVia.equals("residential")
                || tipoVia.equals("living_street")
                || tipoVia.equals("pedestrian")
                || tipoVia.equals("service")
                || tipoVia.equals("tertiary")
                || tipoVia.equals("secondary");
    }
}
