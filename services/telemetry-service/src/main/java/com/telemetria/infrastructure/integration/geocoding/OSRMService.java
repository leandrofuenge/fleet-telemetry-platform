package com.telemetria.infrastructure.integration.geocoding;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OSRMService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&addressdetails=1";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retorna o endereço completo como JsonNode
     */
    public JsonNode reverseGeocode(Double latitude, Double longitude) {
        try {
            String url = String.format(NOMINATIM_URL, latitude, longitude);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "MeuSistemaTelemetria/1.0");
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retorna o endereço como objeto EnderecoInfo
     */
    public EnderecoInfo getEnderecoInfo(Double latitude, Double longitude) {
        JsonNode json = reverseGeocode(latitude, longitude);
        return json != null ? new EnderecoInfo(json) : null;
    }

    /**
     * Método para compatibilidade com código existente
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public java.util.Map<String, Object> reverseGeocodeLegacy(Double latitude, Double longitude) {
        JsonNode json = reverseGeocode(latitude, longitude);
        if (json == null) return null;
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("display_name", json.has("display_name") ? json.get("display_name").asText() : null);
        
        if (json.has("address")) {
            JsonNode address = json.get("address");
            java.util.Map<String, Object> addressMap = new java.util.HashMap<>();
            
            if (address.has("city")) addressMap.put("city", address.get("city").asText());
            if (address.has("town")) addressMap.put("town", address.get("town").asText());
            if (address.has("village")) addressMap.put("village", address.get("village").asText());
            if (address.has("state")) addressMap.put("state", address.get("state").asText());
            if (address.has("country")) addressMap.put("country", address.get("country").asText());
            if (address.has("road")) addressMap.put("road", address.get("road").asText());
            if (address.has("postcode")) addressMap.put("postcode", address.get("postcode").asText());
            if (address.has("suburb")) addressMap.put("suburb", address.get("suburb").asText());
            if (address.has("highway")) addressMap.put("highway", address.get("highway").asText());
            
            map.put("address", addressMap);
        }
        
        return map;
    }
}