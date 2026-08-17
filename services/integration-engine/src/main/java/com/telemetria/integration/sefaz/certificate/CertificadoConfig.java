package com.telemetria.integration.sefaz.certificate;

import java.io.File;

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.telemetria.integration.config.SefazProperties;

@Configuration
public class CertificadoConfig {

    private static final Logger log = LoggerFactory.getLogger(CertificadoConfig.class);
    private final SefazProperties sefazProperties;

    public CertificadoConfig(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    /**
     * Cria e registra o bean SSLContextParameters que o Camel utiliza para mTLS nas chamadas aos WebServices da SEFAZ.
     */
    @Bean(name = "sefazSslContextParameters")
    public SSLContextParameters sefazSslContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        String certPath = sefazProperties.getCertificado().getArquivo();
        String certPassword = sefazProperties.getCertificado().getSenha();

        if (certPath != null && !certPath.isBlank() && new File(certPath).exists()) {
            log.info("Configurando SSLContextParameters do Camel com certificado A1: {}", certPath);

            KeyStoreParameters ksp = new KeyStoreParameters();
            ksp.setResource(certPath);
            ksp.setPassword(certPassword != null ? certPassword : "");
            ksp.setType("PKCS12");

            KeyManagersParameters kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(certPassword != null ? certPassword : "");

            sslContextParameters.setKeyManagers(kmp);

            TrustManagersParameters tmp = new TrustManagersParameters();
            tmp.setKeyStore(ksp);
            sslContextParameters.setTrustManagers(tmp);
        } else {
            log.info("Nenhum certificado SEFAZ A1 customizado informado. Utilizando SSLContext padrao para testes.");
        }

        return sslContextParameters;
    }
}
