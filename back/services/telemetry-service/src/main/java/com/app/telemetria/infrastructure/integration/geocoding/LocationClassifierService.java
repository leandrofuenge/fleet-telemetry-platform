package com.app.telemetria.infrastructure.integration.geocoding;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class LocationClassifierService {

    private final OSMRService osmrService;  // Nome alterado para OSMRService

    public LocationClassifierService(OSMRService osmrService) {  // Parâmetro alterado
        this.osmrService = osmrService;
    }

    public String classify(Double latitude, Double longitude) {

        // Usa o método legacy que retorna Map para compatibilidade
        Map<String, Object> response = osmrService.reverseGeocodeLegacy(latitude, longitude);  // Nome do método alterado

        if (response == null) return "DESCONHECIDO";

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) response.get("address");

        if (address == null) return "DESCONHECIDO";

        String road = (String) address.get("road");
        String city = (String) address.get("city");
        String town = (String) address.get("town");
        String village = (String) address.get("village");
        String highway = (String) address.get("highway");

        // 🚛 Rodovia
        if (highway != null &&
                (highway.contains("motorway")
                        || highway.contains("trunk")
                        || highway.contains("primary"))) {
            return "RODOVIA";
        }

        // 🏙 Área urbana
        if (city != null || town != null || village != null) {
            return "AREA_URBANA";
        }

        if (road != null && road.toLowerCase().contains("rua")) {
            return "AREA_URBANA";
        }

        return "RODOVIA";
    }
}