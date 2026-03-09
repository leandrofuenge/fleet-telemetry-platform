package com.app.routing.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OsrmClient {

    private final WebClient.Builder builder;

    private static final String OSRM_URL = "http://host.docker.internal:5000";
    // IMPORTANTE: se estiver rodando dentro do Docker

    public OsrmClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    public String calcularRota(Double origemLat,
                               Double origemLon,
                               Double destinoLat,
                               Double destinoLon) {

        String url = OSRM_URL + "/route/v1/driving/"
                + origemLon + "," + origemLat + ";"
                + destinoLon + "," + destinoLat
                + "?overview=full&geometries=geojson";

        System.out.println("🔵 Chamando OSRM URL:");
        System.out.println(url);

        String response = builder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("🟢 Resposta bruta OSRM:");
        System.out.println(response);

        return response;
    }
}