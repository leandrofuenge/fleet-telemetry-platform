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
 * Em caso de indisponibilidade do OSRM, lança RuntimeException explícita.
 * NUNCA retorna lista vazia silenciosamente — isso equivaleria a usar "linha reta".
 */
@Service
public class OSRMroutingService {

    private static final Logger log = LoggerFactory.getLogger(OSRMroutingService.class);

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
}