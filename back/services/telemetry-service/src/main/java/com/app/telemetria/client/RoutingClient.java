package com.app.telemetria.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.app.telemetria.dto.RouteResponse;

@Component
public class RoutingClient {

    private final WebClient.Builder builder;

    private static final String ROUTING_SERVICE_URL = "http://localhost:8082"; 
    // porta do routing-service

    public RoutingClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    public RouteResponse calcular(Double origemLat,
            Double origemLon,
            Double destinoLat,
            Double destinoLon) {

   return builder
                    .baseUrl(ROUTING_SERVICE_URL)
                    .build()
                    .get()
                     .uri(uriBuilder -> uriBuilder
                                 .path("/api/routing/calcular")
                                 .queryParam("origemLat", origemLat)
                                 .queryParam("origemLon", origemLon)
                                 .queryParam("destinoLat", destinoLat)
                                 .queryParam("destinoLon", destinoLon)
                                 .build())
                   .retrieve()
                   .bodyToMono(RouteResponse.class)
                   .block();
     }
}