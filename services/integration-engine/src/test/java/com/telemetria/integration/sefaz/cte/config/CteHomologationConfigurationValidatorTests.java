package com.telemetria.integration.sefaz.cte.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;
import com.telemetria.integration.sefaz.cte.exception.CteException;

class CteHomologationConfigurationValidatorTests {

    @Test
    void deveAceitarConfiguracaoCompletaDeHomologacao() throws Exception {
        Fixture fixture = fixtureValida();
        assertDoesNotThrow(() -> fixture.validator.run(null));
    }

    @Test
    void deveRejeitarEndpointNaoHttps() throws Exception {
        Fixture fixture = fixtureValida();
        fixture.properties.getCte().getEndpoints().setEvento(URI.create("http://localhost/evento"));
        assertThrows(CteException.class, () -> fixture.validator.run(null));
    }

    @Test
    void deveRejeitarModoSimulado() throws Exception {
        Fixture fixture = fixtureValida();
        when(fixture.environment.getProperty("integration.simulation.enabled", Boolean.class, false))
                .thenReturn(true);
        assertThrows(CteException.class, () -> fixture.validator.run(null));
    }

    private Fixture fixtureValida() throws Exception {
        SefazProperties properties = new SefazProperties();
        properties.getCte().setAmbiente("homologacao");
        properties.getCte().setVersao("4.00");
        properties.getCte().getEndpoints().setAutorizacao(URI.create("https://sefaz.test/autorizar"));
        properties.getCte().getEndpoints().setConsulta(URI.create("https://sefaz.test/consultar"));
        properties.getCte().getEndpoints().setEvento(URI.create("https://sefaz.test/evento"));
        properties.getCte().getEndpoints().setStatus(URI.create("https://sefaz.test/status"));
        properties.getCertificado().setArquivo("sefaz-a1.p12");
        properties.getCertificado().setSenha("senha-real");

        PrivateKey privateKey = mock(PrivateKey.class);
        X509Certificate certificate = mock(X509Certificate.class);
        KeyStore keyStore = mock(KeyStore.class);
        when(keyStore.aliases()).thenReturn(Collections.enumeration(Collections.singleton("a1")));
        when(keyStore.getKey("a1", "senha-real".toCharArray())).thenReturn(privateKey);
        when(keyStore.getCertificate("a1")).thenReturn(certificate);

        CertificadoLoader loader = mock(CertificadoLoader.class);
        when(loader.carregarKeyStore("sefaz-a1.p12", "senha-real", "PKCS12")).thenReturn(keyStore);
        Environment environment = mock(Environment.class);
        when(environment.getProperty("integration.simulation.enabled", Boolean.class, false)).thenReturn(false);

        return new Fixture(properties, environment,
                new CteHomologationConfigurationValidator(properties, loader, environment));
    }

    private record Fixture(SefazProperties properties, Environment environment,
            CteHomologationConfigurationValidator validator) {}
}
