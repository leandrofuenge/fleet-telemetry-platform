package com.app.telemetria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@Service
public class OpenStreetMapService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> reverseGeocode(double lat, double lon) {

        String url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=" + lat +
                "&lon=" + lon +
                "&format=json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "meu-sistema/1.0");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }
}