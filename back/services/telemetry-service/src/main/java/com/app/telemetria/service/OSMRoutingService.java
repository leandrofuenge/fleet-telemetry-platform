package com.app.telemetria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OSMRoutingService {

    private final RestTemplate restTemplate = new RestTemplate();

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

        ResponseEntity<Map> response =
                restTemplate.getForEntity(url, Map.class);

        Map body = response.getBody();

        if (body == null) return new ArrayList<>();

        List routes = (List) body.get("routes");
        if (routes.isEmpty()) return new ArrayList<>();

        Map firstRoute = (Map) routes.get(0);
        Map geometry = (Map) firstRoute.get("geometry");

        List coordinates = (List) geometry.get("coordinates");

        List<double[]> pontos = new ArrayList<>();

        for (Object obj : coordinates) {
            List coord = (List) obj;

            double lon = ((Number) coord.get(0)).doubleValue();
            double lat = ((Number) coord.get(1)).doubleValue();

            pontos.add(new double[]{lat, lon});
        }

        return pontos;
    }
}