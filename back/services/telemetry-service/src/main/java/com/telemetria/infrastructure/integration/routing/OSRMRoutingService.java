package com.telemetria.infrastructure.integration.routing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RN-ROT-001 — Integração com OSRM para obtenção de trajeto real.
 * RN-ROT-002 — Obtenção do tipo de via para classificação de tolerância.
 * Em caso de indisponibilidade do OSRM, lança RuntimeException explícita.
 * NUNCA retorna lista vazia silenciosamente — isso equivaleria a usar "linha reta".
 */
@Service
public class OSRMRoutingService {

    private static final Logger log = LoggerFactory.getLogger(OSRMRoutingService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtém os pontos do trajeto real via OSRM.
     *
     * @throws RuntimeException se o OSRM estiver indisponível (RN-ROT-001)
     */
    public List<double[]> obterRota(
            double latOrigem,
            double lonOrigem,
            double latDestino,
            double lonDestino) {

        // RN-ROT-001: perfil caminhao por padrão na URL
        String url = String.format(
                "http://192.168.1.12:5000/route/v1/caminhao/%f,%f;%f,%f?overview=full&geometries=geojson",
                lonOrigem, latOrigem,
                lonDestino, latDestino
        );

        log.info("🔵 [RN-ROT-001] Chamando OSRM: {}", url);

        String responseJson;
        try {
            responseJson = restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            // RN-ROT-001: NUNCA retornar silenciosamente — lançar erro explícito.
            log.error("❌ [RN-ROT-001] OSRM indisponível: {}. Rota não pode ser calculada.", e.getMessage());
            throw new RuntimeException(
                    "[RN-ROT-001] OSRM indisponível. Rota não pode ser usada sem cálculo real de trajeto.", e);
        }

        if (responseJson == null || responseJson.isBlank()) {
            log.error("❌ [RN-ROT-001] OSRM retornou resposta vazia.");
            throw new RuntimeException("[RN-ROT-001] OSRM retornou resposta vazia. Rota não pode ser ativada.");
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);

            List<double[]> pontos = new ArrayList<>();

            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) {
                log.warn("⚠️ OSRM retornou routes vazio para o trajeto solicitado.");
                return pontos;
            }

            JsonNode geometry = routes.get(0).path("geometry");
            JsonNode coordinates = geometry.path("coordinates");

            for (JsonNode coord : coordinates) {
                double lon = coord.get(0).asDouble();
                double lat = coord.get(1).asDouble();
                pontos.add(new double[]{lat, lon});
            }

            log.info("✅ [RN-ROT-001] Trajeto obtido: {} pontos.", pontos.size());
            return pontos;

        } catch (Exception e) {
            log.error("❌ Erro ao interpretar resposta do OSRM: {}", e.getMessage());
            throw new RuntimeException("Erro ao interpretar resposta do OSRM.", e);
        }
    }

    /**
     * RN-ROT-002: Obtém o tipo de via (highway type) para uma coordenada específica
     * Utiliza o endpoint nearest do OSRM para obter informações da via mais próxima
     * 
     * @param latitude Latitude da posição
     * @param longitude Longitude da posição
     * @return Tipo de via (motorway, primary, residential, etc) ou null se não encontrado
     */
    public String getHighwayType(double latitude, double longitude) {
        String url = String.format(
                "http://192.168.1.12:5000/nearest/v1/caminhao/%f,%f?number=1",
                longitude, latitude
        );

        log.debug("🔍 [RN-ROT-002] Consultando tipo de via OSRM: {}", url);

        try {
            String responseJson = restTemplate.getForObject(url, String.class);
            
            if (responseJson == null || responseJson.isBlank()) {
                log.warn("⚠️ [RN-ROT-002] OSRM retornou resposta vazia para nearest");
                return null;
            }

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode ways = root.path("ways");
            
            if (ways.isEmpty()) {
                log.debug("ℹ️ [RN-ROT-002] Nenhuma via encontrada para as coordenadas");
                return null;
            }

            // Pega a primeira via (mais próxima)
            JsonNode primeiraVia = ways.get(0);
            String highwayType = primeiraVia.path("tags").path("highway").asText(null);
            
            if (highwayType != null && !highwayType.isEmpty()) {
                log.debug("✅ [RN-ROT-002] Tipo de via identificado: {}", highwayType);
                return highwayType;
            }
            
            // Fallback: tentar obter do atributo name ou class
            String wayClass = primeiraVia.path("tags").path("class").asText(null);
            if (wayClass != null && !wayClass.isEmpty()) {
                log.debug("✅ [RN-ROT-002] Classe da via identificada: {}", wayClass);
                return wayClass;
            }
            
        } catch (RestClientException e) {
            log.warn("⚠️ [RN-ROT-002] Erro ao consultar OSRM para tipo de via: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("⚠️ [RN-ROT-002] Erro ao processar resposta do OSRM: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * RN-ROT-002: Obtém informações completas da via para uma coordenada
     * 
     * @param latitude Latitude da posição
     * @param longitude Longitude da posição
     * @return JsonNode com informações da via ou null
     */
    public JsonNode getRoadInfo(double latitude, double longitude) {
        String url = String.format(
                "http://192.168.1.12:5000/nearest/v1/caminhao/%f,%f?number=1",
                longitude, latitude
        );

        try {
            String responseJson = restTemplate.getForObject(url, String.class);
            
            if (responseJson == null || responseJson.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode ways = root.path("ways");
            
            if (ways.isEmpty()) {
                return null;
            }

            return ways.get(0);
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao obter informações da via: {}", e.getMessage());
            return null;
        }
    }

    /**
     * RN-ROT-002: Verifica se a coordenada está em uma rodovia
     */
    public boolean isRodovia(double latitude, double longitude) {
        String highwayType = getHighwayType(latitude, longitude);
        if (highwayType == null) {
            return false;
        }
        
        String tipo = highwayType.toLowerCase();
        return tipo.equals("motorway") || tipo.equals("trunk") || 
               tipo.equals("primary") || tipo.equals("motorway_link") ||
               tipo.equals("trunk_link");
    }

    /**
     * RN-ROT-002: Verifica se a coordenada está em área urbana
     */
    public boolean isAreaUrbana(double latitude, double longitude) {
        String highwayType = getHighwayType(latitude, longitude);
        if (highwayType == null) {
            return false;
        }
        
        String tipo = highwayType.toLowerCase();
        return tipo.equals("residential") || tipo.equals("living_street") || 
               tipo.equals("pedestrian") || tipo.equals("service") ||
               tipo.equals("tertiary") || tipo.equals("secondary");
    }
}