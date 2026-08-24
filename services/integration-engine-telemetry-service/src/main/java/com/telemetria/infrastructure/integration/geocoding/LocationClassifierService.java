package com.telemetria.infrastructure.integration.geocoding;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LocationClassifierService {

    private static final Logger log = LoggerFactory.getLogger(LocationClassifierService.class);

    private final OSRMService osrmService;

    public LocationClassifierService(OSRMService osrmService) {
        this.osrmService = osrmService;
    }

    /**
     * Reutiliza a engine de classificação baseada em OSRM/OpenStreetMap 
     * para responder se o veículo está ou não em perímetro urbano.
     */
    public boolean verificarAreaUrbana(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        
        String classificacao = classify(latitude, longitude);
        log.debug("🗺️ Coordenadas [Lat: {}, Long: {}] classificadas como: {}", latitude, longitude, classificacao);
        
        return "AREA_URBANA".equals(classificacao);
    }

    /**
     * Classifica a região com base nas propriedades geográficas retornadas pelo OSRM.
     */
    public String classify(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "DESCONHECIDO";
        }

        Map<String, Object> response = osrmService.reverseGeocodeLegacy(latitude, longitude);
        if (response == null) {
            return "DESCONHECIDO";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) response.get("address");
        if (address == null) {
            return "DESCONHECIDO";
        }

        String highway = get(address, "highway");
        String road = get(address, "road");
        String city = get(address, "city");
        String town = get(address, "town");
        String village = get(address, "village");
        String suburb = get(address, "suburb");
        String hamlet = get(address, "hamlet");

        // 1. Validação por Rodovias Principais
        if (highway != null) {
            String h = highway.toLowerCase();

            if (h.contains("motorway")
                    || h.contains("trunk")
                    || h.contains("primary")
                    || h.contains("secondary")) {
                return "RODOVIA";
            }

            // Ruas urbanas secundárias
            if (h.contains("residential")
                    || h.contains("living_street")
                    || h.contains("service")) {
                return "AREA_URBANA";
            }
        }

        // 2. Presença de estruturas ou divisões urbanas explicitadas
        if (city != null || town != null || suburb != null) {
            return "AREA_URBANA";
        }

        // 3. Pequenos povoados / Distritos rurais
        if (village != null || hamlet != null) {
            return "AREA_RURAL";
        }

        // 4. Fallback pelo nome da via em português
        if (road != null) {
            String r = road.toLowerCase();

            if (r.contains("rua")
                    || r.contains("avenida")
                    || r.contains("travessa")
                    || r.contains("alameda")) {
                return "AREA_URBANA";
            }
        }

        return "DESCONHECIDO";
    }

    private String get(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}