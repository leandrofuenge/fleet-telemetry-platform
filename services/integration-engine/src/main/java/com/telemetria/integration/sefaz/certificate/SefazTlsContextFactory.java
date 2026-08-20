package com.telemetria.integration.sefaz.certificate;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.exception.CteException;

/** Cria o contexto mTLS usando stores independentes para identidade e confiança. */
@Component
public class SefazTlsContextFactory {

    private final CertificadoLoader loader;
    private final SefazProperties properties;

    public SefazTlsContextFactory(CertificadoLoader loader, SefazProperties properties) {
        this.loader = loader;
        this.properties = properties;
    }

    public SSLContext criar() {
        try {
            var certificado = properties.getCertificado();
            if (certificado.getArquivo() == null || certificado.getArquivo().isBlank()) {
                return SSLContext.getDefault();
            }

            KeyStore clientKeyStore = loader.carregarKeyStore(
                    certificado.getArquivo(), certificado.getSenha(), certificado.getTipo());
            char[] keyPassword = senhaChave(certificado).toCharArray();
            KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(clientKeyStore, keyPassword);

            TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            var truststore = properties.getTls().getTruststore();
            if (truststore.getArquivo() == null || truststore.getArquivo().isBlank()) {
                // Cadeias públicas confiáveis do runtime Java; nunca usa o A1 como truststore.
                trustManagers.init((KeyStore) null);
            } else {
                KeyStore trustKeyStore = loader.carregarKeyStore(
                        truststore.getArquivo(), truststore.getSenha(), truststore.getTipo());
                trustManagers.init(trustKeyStore);
            }

            SSLContext context = SSLContext.getInstance(properties.getTls().getProtocolo());
            context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new CteException("Não foi possível inicializar o mTLS da SEFAZ.", e);
        }
    }

    private String senhaChave(SefazProperties.Certificado certificado) {
        return certificado.getSenhaChave() == null || certificado.getSenhaChave().isBlank()
                ? valorOuVazio(certificado.getSenha())
                : certificado.getSenhaChave();
    }

    private String valorOuVazio(String value) {
        return value == null ? "" : value;
    }
}
