package com.app.telemetria.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.cache.annotation.Cacheable;
import java.util.Optional;

@Service
public class IPLocateService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    
    public IPLocateService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${iplocate.api.key}") String apiKey,
            @Value("${iplocate.api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IPLocateResponse {
        private String ip;
        private String country;
        private String countryCode;
        private String region;
        private String city;
        private Double latitude;
        private Double longitude;
        private String timeZone;
        private String isp;
        private String organization;
        private Boolean isProxy;
        private Boolean isTorExit;
        private String postalCode;
        
        // Getters e Setters
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        
        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
        
        public String getIsp() { return isp; }
        public void setIsp(String isp) { this.isp = isp; }
        
        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
        
        public Boolean getIsProxy() { return isProxy; }
        public void setIsProxy(Boolean isProxy) { this.isProxy = isProxy; }
        
        public Boolean getIsTorExit() { return isTorExit; }
        public void setIsTorExit(Boolean isTorExit) { this.isTorExit = isTorExit; }
        
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    }
    
    public record LocalizacaoInfo(
        String ip,
        String cidade,
        String estado,
        String pais,
        Double latitude,
        Double longitude,
        String provedor,
        boolean usandoProxy
    ) {}
    
    /**
     * Busca localização completa a partir do IP
     */
    @Cacheable(value = "iplocate", key = "#ip", unless = "#result == null")
    public Optional<LocalizacaoInfo> buscarLocalizacaoPorIP(String ip) {
        try {
            String url = String.format("%s/%s?apikey=%s", apiUrl, ip, apiKey);
            var response = restTemplate.getForObject(url, IPLocateResponse.class);
            
            if (response != null && response.getLatitude() != null) {
                return Optional.of(new LocalizacaoInfo(
                    response.getIp(),
                    response.getCity(),
                    response.getRegion(),
                    response.getCountry(),
                    response.getLatitude(),
                    response.getLongitude(),
                    response.getOrganization(),
                    response.getIsProxy() != null && response.getIsProxy()
                ));
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Erro IPLocate: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Busca localização do IP da requisição atual
     */
    public Optional<LocalizacaoInfo> buscarLocalizacaoAtual() {
        return buscarLocalizacaoPorIP(null); // null = IP da requisição
    }
}