package com.telemetria.integration.sefaz.certificate;

import javax.net.ssl.SSLContext;

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.telemetria.integration.config.SefazProperties;

@Configuration
public class CertificadoConfig {

    @Bean(name = "sefazSslContext")
    public SSLContext sefazSslContext(SefazTlsContextFactory factory) {
        return factory.criar();
    }

    /** Contexto equivalente para os componentes HTTP do Camel. */
    @Bean(name = "sefazSslContextParameters")
    public SSLContextParameters sefazSslContextParameters(SefazProperties properties) {
        SSLContextParameters parameters = new SSLContextParameters();
        var certificado = properties.getCertificado();

        if (preenchido(certificado.getArquivo())) {
            KeyStoreParameters clientStore = store(certificado.getArquivo(), certificado.getSenha(),
                    certificado.getTipo());
            KeyManagersParameters keyManagers = new KeyManagersParameters();
            keyManagers.setKeyStore(clientStore);
            keyManagers.setKeyPassword(preenchido(certificado.getSenhaChave())
                    ? certificado.getSenhaChave() : valorOuVazio(certificado.getSenha()));
            parameters.setKeyManagers(keyManagers);
        }

        var truststore = properties.getTls().getTruststore();
        if (preenchido(truststore.getArquivo())) {
            TrustManagersParameters trustManagers = new TrustManagersParameters();
            trustManagers.setKeyStore(store(truststore.getArquivo(), truststore.getSenha(), truststore.getTipo()));
            parameters.setTrustManagers(trustManagers);
        }

        SecureSocketProtocolsParameters protocols = new SecureSocketProtocolsParameters();
        protocols.getSecureSocketProtocol().add(properties.getTls().getProtocolo());
        parameters.setSecureSocketProtocols(protocols);
        return parameters;
    }

    private KeyStoreParameters store(String arquivo, String senha, String tipo) {
        KeyStoreParameters store = new KeyStoreParameters();
        store.setResource(arquivo);
        store.setPassword(valorOuVazio(senha));
        store.setType(preenchido(tipo) ? tipo : "PKCS12");
        return store;
    }

    private boolean preenchido(String value) {
        return value != null && !value.isBlank();
    }

    private String valorOuVazio(String value) {
        return value == null ? "" : value;
    }
}
