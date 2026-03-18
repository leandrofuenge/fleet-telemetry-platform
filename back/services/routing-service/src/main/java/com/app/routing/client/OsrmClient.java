package com.app.routing.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OsrmClient {

    private final WebClient.Builder builder;

    private static final String OSRM_URL = "http://192.168.1.12:5000";
    // IMPORTANTE: se estiver rodando dentro do Docker

    public OsrmClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    public String calcularRota(Double origemLat,
                               Double origemLon,
                               Double destinoLat,
                               Double destinoLon) {

        // CORREÇÃO: OSRM espera LONGITUDE,LATITUDE (não latitude,longitude)
        String url = OSRM_URL + "/route/v1/driving/"
                + origemLon + "," + origemLat + ";"  // ← ordem corrigida
                + destinoLon + "," + destinoLat       // ← ordem corrigida
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