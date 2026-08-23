package com.telemetria.integration.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.transport")
public record TransportSecurityProperties(
        boolean requireHttps,
        boolean hstsEnabled,
        long hstsMaxAgeSeconds,
        boolean hstsIncludeSubdomains) {
}
