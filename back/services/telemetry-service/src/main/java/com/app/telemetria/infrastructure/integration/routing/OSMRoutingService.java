package com.app.telemetria.infrastructure.integration.routing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OSMRoutingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<double[]> obterRota(
            double latOrigem,
            double lonOrigem,
            double latDestino,
            double lonDestino) {

        String url = String.format(
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                lonOrigem, latOrigem,
                lonDestino, latDestino
        );

        try {
            String responseJson = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(responseJson);
            
            List<double[]> pontos = new ArrayList<>();
            
            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) return pontos;
            
            JsonNode geometry = routes.get(0).path("geometry");
            JsonNode coordinates = geometry.path("coordinates");
            
            for (JsonNode coord : coordinates) {
                double lon = coord.get(0).asDouble();
                double lat = coord.get(1).asDouble();
                pontos.add(new double[]{lat, lon});
            }
            
            return pontos;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}