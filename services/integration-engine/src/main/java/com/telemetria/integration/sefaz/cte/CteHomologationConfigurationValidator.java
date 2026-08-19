package com.telemetria.integration.sefaz.cte;

import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;

/** Impede que o perfil CT-e de homologação suba com configuração parcial ou simulada. */
@Component
@Profile("cte-homologation")
public class CteHomologationConfigurationValidator implements ApplicationRunner {

    private final SefazProperties properties;
    private final CertificadoLoader certificadoLoader;
    private final Environment environment;

    public CteHomologationConfigurationValidator(SefazProperties properties,
            CertificadoLoader certificadoLoader, Environment environment) {
        this.properties = properties;
        this.certificadoLoader = certificadoLoader;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validarAmbiente();
        validarEndpoints();
        validarCertificadoA1();
        validarTruststore();
        validarExecucaoReal();
    }

    private void validarAmbiente() {
        if (properties.getCte().ambienteCte() != CteAmbiente.HOMOLOGACAO) {
            falhar("sefaz.cte.ambiente deve ser homologacao (tpAmb=2).");
        }
        if (!"4.00".equals(properties.getCte().getVersao())) {
            falhar("sefaz.cte.versao deve ser 4.00.");
        }
    }

    private void validarEndpoints() {
        var endpoints = properties.getCte().getEndpoints();
        List.of(
                new Endpoint("autorizacao", endpoints.getAutorizacao()),
                new Endpoint("consulta", endpoints.getConsulta()),
                new Endpoint("evento", endpoints.getEvento()),
                new Endpoint("status", endpoints.getStatus()))
                .forEach(this::validarEndpoint);
    }

    private void validarEndpoint(Endpoint endpoint) {
        URI uri = endpoint.uri();
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            falhar("Endpoint " + endpoint.nome() + " deve ser uma URL HTTPS absoluta.");
        }
        String value = uri.toString().toLowerCase();
        if (value.contains("change_me") || value.contains("localhost") || value.contains("example")) {
            falhar("Endpoint " + endpoint.nome() + " contém valor placeholder ou host local.");
        }
    }

    private void validarCertificadoA1() {
        var certificado = properties.getCertificado();
        exigirPreenchido(certificado.getArquivo(), "SEFAZ_CERT_PATH");
        exigirPreenchido(certificado.getSenha(), "SEFAZ_CERT_PASSWORD");
        try {
            KeyStore keyStore = certificadoLoader.carregarKeyStore(
                    certificado.getArquivo(), certificado.getSenha(), certificado.getTipo());
            String keyPasswordValue = preenchido(certificado.getSenhaChave())
                    ? certificado.getSenhaChave() : certificado.getSenha();
            char[] keyPassword = keyPasswordValue.toCharArray();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Key key = keyStore.getKey(alias, keyPassword);
                if (key instanceof PrivateKey && keyStore.getCertificate(alias) instanceof X509Certificate certificate) {
                    certificate.checkValidity();
                    return;
                }
            }
            falhar("Certificado A1 não contém chave privada e certificado X.509 utilizáveis.");
        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Configuração CT-e homologation inválida: certificado A1 não pôde ser validado.", e);
        }
    }

    private void validarTruststore() {
        var truststore = properties.getTls().getTruststore();
        if (!preenchido(truststore.getArquivo())) {
            return; // Usa o truststore padrão e atualizado do runtime Java.
        }
        try {
            KeyStore store = certificadoLoader.carregarKeyStore(
                    truststore.getArquivo(), truststore.getSenha(), truststore.getTipo());
            if (store.size() == 0) {
                falhar("Truststore SEFAZ configurado não possui certificados confiáveis.");
            }
        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Configuração CT-e homologation inválida: truststore não pôde ser validado.", e);
        }
    }

    private void validarExecucaoReal() {
        if (environment.getProperty("integration.simulation.enabled", Boolean.class, false)) {
            falhar("integration.simulation.enabled deve ser false.");
        }
        exigirPreenchido(properties.getTls().getProtocolo(), "SEFAZ_TLS_PROTOCOL");
    }

    private void exigirPreenchido(String value, String variable) {
        if (!preenchido(value) || value.startsWith("CHANGE_ME")) {
            falhar(variable + " deve ser preenchida com valor real.");
        }
    }

    private boolean preenchido(String value) {
        return value != null && !value.isBlank();
    }

    private void falhar(String message) {
        throw new CteException("Configuração CT-e homologation inválida: " + message);
    }

    private record Endpoint(String nome, URI uri) {}
}
