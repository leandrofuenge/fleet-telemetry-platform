package com.telemetria.integration.sefaz.cte.config;

import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;
import com.telemetria.integration.sefaz.cte.domain.CteAmbiente;
import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Validador de inicialização (Fail-Fast) para o perfil de homologação CT-e.
 * Impede que a aplicação suba com configurações incompletas, simuladas ou certificados inválidos.
 */
@Component
@Profile("cte-homologation")
public class CteHomologationConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CteHomologationConfigurationValidator.class);

    private final SefazProperties properties;
    private final CertificadoLoader certificadoLoader;
    private final Environment environment;

    public CteHomologationConfigurationValidator(SefazProperties properties,
                                                 CertificadoLoader certificadoLoader,
                                                 Environment environment) {
        this.properties = properties;
        this.certificadoLoader = certificadoLoader;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Iniciando validação estrita de inicialização para o perfil CT-e Homologação...");

        validarAmbiente();
        validarEndpoints();
        validarCertificadoA1();
        validarTruststore();
        validarExecucaoReal();

        log.info("Validação de inicialização do perfil CT-e Homologação concluída com sucesso.");
    }

    private void validarAmbiente() {
        var cteProps = Optional.ofNullable(properties.getCte())
                .orElseThrow(() -> new CteException("Configuração 'sefaz.cte' ausente."));

        if (cteProps.ambienteCte() != CteAmbiente.HOMOLOGACAO) {
            falhar("sefaz.cte.ambiente deve ser homologacao (tpAmb=2).");
        }
        if (!"4.00".equals(cteProps.getVersao())) {
            falhar("sefaz.cte.versao deve ser 4.00.");
        }
    }

    private void validarEndpoints() {
        var endpoints = Optional.ofNullable(properties.getCte())
                .map(SefazProperties.Cte::getEndpoints)
                .orElseThrow(() -> new CteException("Configuração 'sefaz.cte.endpoints' ausente."));

        List.of(
                new Endpoint("autorizacao", endpoints.getAutorizacao()),
                new Endpoint("consulta", endpoints.getConsulta()),
                new Endpoint("evento", endpoints.getEvento()),
                new Endpoint("status", endpoints.getStatus())
        ).forEach(this::validarEndpoint);
    }

    private void validarEndpoint(Endpoint endpoint) {
        URI uri = endpoint.uri();
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            falhar("Endpoint " + endpoint.nome() + " deve ser uma URL HTTPS absoluta.");
        }
        String value = uri.toString().toLowerCase();
        if (value.contains("change_me") || value.contains("localhost") || value.contains("example")) {
            falhar("Endpoint " + endpoint.nome() + " contém valor placeholder ou host local não permitido em homologação.");
        }
    }

    private void validarCertificadoA1() {
        var certificado = Optional.ofNullable(properties.getCertificado())
                .orElseThrow(() -> new CteException("Configuração 'sefaz.certificado' ausente."));

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

                if (key instanceof PrivateKey && keyStore.getCertificate(alias) instanceof X509Certificate cert) {
                    try {
                        cert.checkValidity();
                        log.info("Certificado A1 carregado e válido. Alias: '{}' | Expira em: {}", alias, cert.getNotAfter());
                        return;
                    } catch (CertificateExpiredException e) {
                        falhar("Certificado A1 expirado em: " + cert.getNotAfter());
                    } catch (CertificateNotYetValidException e) {
                        falhar("Certificado A1 ainda não é válido. Início de validade: " + cert.getNotBefore());
                    }
                }
            }
            falhar("Certificado A1 não contém um par de chave privada e certificado X.509 válido.");

        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Configuração CT-e homologation inválida: falha ao validar o certificado A1.", e);
        }
    }

    private void validarTruststore() {
        var truststore = Optional.ofNullable(properties.getTls())
                .map(SefazProperties.Tls::getTruststore)
                .orElse(null);

        if (truststore == null || !preenchido(truststore.getArquivo())) {
            log.debug("Truststore customizado não configurado. Utilizando CA Store padrão do Java.");
            return;
        }

        try {
            KeyStore store = certificadoLoader.carregarKeyStore(
                    truststore.getArquivo(), truststore.getSenha(), truststore.getTipo());

            if (store.size() == 0) {
                falhar("Truststore SEFAZ configurado em " + truststore.getArquivo() + " está vazio.");
            }
            log.info("Truststore carregado com sucesso. {} certificados confiáveis encontrados.", store.size());

        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Configuração CT-e homologation inválida: truststore não pôde ser validado.", e);
        }
    }

    private void validarExecucaoReal() {
        if (environment.getProperty("integration.simulation.enabled", Boolean.class, false)) {
            falhar("A propriedade 'integration.simulation.enabled' deve ser 'false' no perfil de homologação.");
        }

        var tls = Optional.ofNullable(properties.getTls())
                .orElseThrow(() -> new CteException("Configuração 'sefaz.tls' ausente."));

        exigirPreenchido(tls.getProtocolo(), "SEFAZ_TLS_PROTOCOL");
    }

    private void exigirPreenchido(String value, String variable) {
        if (!preenchido(value) || value.startsWith("CHANGE_ME")) {
            falhar("A variável/propriedade " + variable + " deve ser preenchida com um valor real.");
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
