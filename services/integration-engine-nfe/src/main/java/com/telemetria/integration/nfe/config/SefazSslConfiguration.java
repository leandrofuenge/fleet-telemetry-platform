package com.telemetria.integration.nfe.config;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.telemetria.integration.nfe.security.CertificadoLoader;

/** Cria o contexto TLS usado nas chamadas SOAP da SEFAZ. */
@Configuration
public class SefazSslConfiguration {

    @Bean(name = "sefazSslContext")
    SSLContext sefazSslContext(SefazProperties properties, CertificadoLoader loader) throws Exception {
        SefazProperties.Certificado certificado = properties.getCertificado();
        if (certificado.getArquivo() == null || certificado.getArquivo().isBlank()) {
            return SSLContext.getDefault();
        }

        KeyStore keyStore = loader.carregarKeyStore(
                certificado.getArquivo(), certificado.getSenha(), certificado.getTipo());
        String senhaChave = certificado.getSenhaChave();
        String senhaEfetiva = senhaChave == null || senhaChave.isBlank()
                ? certificado.getSenha() : senhaChave;
        char[] password = senhaEfetiva == null ? new char[0] : senhaEfetiva.toCharArray();

        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, password);
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init((KeyStore) null);

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);
        return context;
    }
}
