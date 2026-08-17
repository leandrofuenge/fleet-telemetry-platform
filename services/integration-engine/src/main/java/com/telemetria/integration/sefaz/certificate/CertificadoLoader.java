package com.telemetria.integration.sefaz.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import org.springframework.stereotype.Component;

@Component
public class CertificadoLoader {

    public KeyStore carregarKeyStore(String caminhoCertificado, String senha) throws Exception {
        if (caminhoCertificado == null || caminhoCertificado.isBlank()) {
            throw new IllegalArgumentException("Caminho do certificado digital não configurado.");
        }
        File file = new File(caminhoCertificado);
        if (!file.exists()) {
            throw new IllegalArgumentException("Arquivo de certificado não encontrado: " + caminhoCertificado);
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = (senha != null) ? senha.toCharArray() : new char[0];
        try (InputStream in = new FileInputStream(file)) {
            keyStore.load(in, password);
        }
        return keyStore;
    }
}
