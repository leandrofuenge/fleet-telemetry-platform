package com.telemetria.integration.senatran.serpro.infrastructure.observability;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;

@Component("serproRadar")
public class SerproHealthIndicator implements HealthIndicator {
    private final SerproProperties properties;

    public SerproHealthIndicator(SerproProperties properties) { this.properties = properties; }

    @Override
    public Health health() {
        boolean tokenConfigured = properties.getToken() != null && !properties.getToken().isBlank();
        boolean apiKeyConfigured = properties.getSerpro().getApiKey() != null
                && !properties.getSerpro().getApiKey().isBlank();
        Health.Builder builder = tokenConfigured && apiKeyConfigured ? Health.up() : Health.outOfService();
        return builder.withDetail("provider", "InfoSimples SERPRO/RADAR")
                .withDetail("providerTokenConfigured", tokenConfigured)
                .withDetail("endpointApiKeyConfigured", apiKeyConfigured)
                .withDetail("activeProbe", false).build();
    }
}
