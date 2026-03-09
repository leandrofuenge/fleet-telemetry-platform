package com.app.telemetria.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LocationClassifierService {

    private final OSMService osmService;

    public LocationClassifierService(OSMService osmService) {
        this.osmService = osmService;
    }

    public String classify(Double latitude, Double longitude) {

        Map<String, Object> response =
                osmService.reverseGeocode(latitude, longitude);

        if (response == null) return "DESCONHECIDO";

        Map<String, Object> address =
                (Map<String, Object>) response.get("address");

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