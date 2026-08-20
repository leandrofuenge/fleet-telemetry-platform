package com.telemetria.integration.senatran.serpro.infrastructure.config;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "infosimples")
public class SerproProperties {

    private String urlSerproRadar = "https://api.infosimples.com/v2/consultas/serpro/radar/veiculo";
    private String token = "";
    private final Serpro serpro = new Serpro();

    public String getUrlSerproRadar() { return urlSerproRadar; }
    public void setUrlSerproRadar(String urlSerproRadar) { this.urlSerproRadar = urlSerproRadar; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Serpro getSerpro() { return serpro; }

    public static class Serpro {
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(10);
        private int maxAttempts = 3;
        private Duration retryDelay = Duration.ofMillis(200);
        private boolean unknownStatusBlocks = true;
        private Set<String> blockingStatuses = new LinkedHashSet<>(Set.of("AUTUADO", "PENDENTE"));
        private int circuitFailureThreshold = 5;
        private Duration circuitOpenDuration = Duration.ofSeconds(30);
        private int requestsPerMinute = 60;
        private Duration cacheTtl = Duration.ofMinutes(5);
        private int cacheMaxEntries = 1000;
        private String apiKey = "";

        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration value) { this.connectTimeout = value; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration value) { this.readTimeout = value; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int value) { this.maxAttempts = Math.max(1, value); }
        public Duration getRetryDelay() { return retryDelay; }
        public void setRetryDelay(Duration value) { this.retryDelay = value; }
        public boolean isUnknownStatusBlocks() { return unknownStatusBlocks; }
        public void setUnknownStatusBlocks(boolean value) { this.unknownStatusBlocks = value; }
        public Set<String> getBlockingStatuses() { return blockingStatuses; }
        public void setBlockingStatuses(Set<String> value) {
            this.blockingStatuses = value == null ? Set.of() : new LinkedHashSet<>(value);
        }
        public int getCircuitFailureThreshold() { return circuitFailureThreshold; }
        public void setCircuitFailureThreshold(int value) { this.circuitFailureThreshold = Math.max(1, value); }
        public Duration getCircuitOpenDuration() { return circuitOpenDuration; }
        public void setCircuitOpenDuration(Duration value) { this.circuitOpenDuration = value; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int value) { this.requestsPerMinute = Math.max(1, value); }
        public Duration getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(Duration value) { this.cacheTtl = value; }
        public int getCacheMaxEntries() { return cacheMaxEntries; }
        public void setCacheMaxEntries(int value) { this.cacheMaxEntries = Math.max(1, value); }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String value) { this.apiKey = value; }
    }
}
