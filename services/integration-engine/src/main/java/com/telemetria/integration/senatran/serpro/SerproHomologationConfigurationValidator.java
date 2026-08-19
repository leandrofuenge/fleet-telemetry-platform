package com.telemetria.integration.senatran.serpro;

import java.net.URI;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Impede um deploy de homologação parcialmente ou perigosamente configurado. */
@Component
@Profile("homologation")
public class SerproHomologationConfigurationValidator implements ApplicationRunner {
    private final SerproProperties properties;

    public SerproHomologationConfigurationValidator(SerproProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireSecret("INFOSIMPLES_TOKEN", properties.getToken());
        requireSecret("INFOSIMPLES_SERPRO_INTERNAL_API_KEY", properties.getSerpro().getApiKey());
        if (properties.getToken().equals(properties.getSerpro().getApiKey())) {
            throw new IllegalStateException("O token do fornecedor e a API key interna devem ser diferentes.");
        }
        URI uri;
        try {
            uri = URI.create(properties.getUrlSerproRadar());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("INFOSIMPLES_SERPRO_RADAR_URL inválida.", e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalStateException("INFOSIMPLES_SERPRO_RADAR_URL deve ser uma URL HTTPS absoluta.");
        }
    }

    private void requireSecret(String name, String value) {
        if (value == null || value.isBlank() || value.length() < 16
                || value.contains("CHANGE_ME") || value.contains("<")) {
            throw new IllegalStateException(name + " deve ser fornecida pelo cofre de segredos e possuir ao menos 16 caracteres.");
        }
    }
}
