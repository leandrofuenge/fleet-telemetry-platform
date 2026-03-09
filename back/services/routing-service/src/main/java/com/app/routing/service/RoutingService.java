package com.app.routing.service;

import com.app.routing.client.OsrmClient;
import com.app.routing.dto.RouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private final OsrmClient osrmClient;
    private final ObjectMapper mapper;

    public RoutingService(OsrmClient osrmClient) {
        this.osrmClient = osrmClient;
        this.mapper = new ObjectMapper();
    }

      public RouteResponse calcularMelhorRota(Double origemLat,
              Double origemLon,
              Double destinoLat,
              Double destinoLon) {

      try {

                      System.out.println("📍 Origem: " + origemLat + ", " + origemLon);
                      System.out.println("📍 Destino: " + destinoLat + ", " + destinoLon);

                     String json = osrmClient.calcularRota(
                                      origemLat,
                                      origemLon,
                                      destinoLat,
                                      destinoLon
             );

              JsonNode node = mapper.readTree(json);
              JsonNode route = node.get("routes").get(0);

             double distanciaMetros = route.get("distance").asDouble();
             double duracaoSegundos = route.get("duration").asDouble();

            System.out.println("📏 Distância (m): " + distanciaMetros);
            System.out.println("⏱ Duração (s): " + duracaoSegundos);

            double distanciaKm = distanciaMetros / 1000.0;
            double duracaoMin = duracaoSegundos / 60.0;

            System.out.println("📏 Distância (km): " + distanciaKm);
            System.out.println("⏱ Duração (min): " + duracaoMin);

            return new RouteResponse(
                              distanciaKm,
                              duracaoMin,
                              route.get("geometry")
           );

       } catch (Exception e) {
           e.printStackTrace();
           throw new RuntimeException("Erro ao calcular rota OSRM", e);
        }
     }
}


