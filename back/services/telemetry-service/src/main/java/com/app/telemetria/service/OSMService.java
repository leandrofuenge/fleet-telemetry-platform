package com.app.telemetria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Service
public class OSMService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> reverseGeocode(Double latitude, Double longitude) {

        String url = String.format(NOMINATIM_URL, latitude, longitude);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "MeuSistemaTelemetria/1.0");

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