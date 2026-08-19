package com.telemetria.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class SefazPropertiesTests {
    @Test
    void vinculaUmaUrlTipadaPorOperacaoCte() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "sefaz.cte.endpoints.autorizacao", "https://sefaz.test/CTeRecepcaoSincV4",
                "sefaz.cte.endpoints.consulta", "https://sefaz.test/CTeConsultaV4",
                "sefaz.cte.endpoints.evento", "https://sefaz.test/CTeRecepcaoEventoV4",
                "sefaz.cte.endpoints.status", "https://sefaz.test/CTeStatusServicoV4"));

        SefazProperties properties = new Binder(source).bind("sefaz", SefazProperties.class)
                .orElseThrow(() -> new AssertionError("Configuração SEFAZ não vinculada"));

        assertThat(properties.getCte().getEndpoints().getAutorizacao())
                .isEqualTo(URI.create("https://sefaz.test/CTeRecepcaoSincV4"));
        assertThat(properties.getCte().getEndpoints().getConsulta())
                .isEqualTo(URI.create("https://sefaz.test/CTeConsultaV4"));
        assertThat(properties.getCte().getEndpoints().getEvento())
                .isEqualTo(URI.create("https://sefaz.test/CTeRecepcaoEventoV4"));
        assertThat(properties.getCte().getEndpoints().getStatus())
                .isEqualTo(URI.create("https://sefaz.test/CTeStatusServicoV4"));
    }
}
