package com.telemetria.integration.nfe.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import org.springframework.stereotype.Component;

@Component
public class CertificadoLoader {

    public KeyStore carregarKeyStore(String caminho, String senha, String tipo) throws Exception {
        if (caminho == null || caminho.isBlank()) {
            throw new IllegalArgumentException("Caminho do certificado digital não configurado.");
        }
        File arquivo = new File(caminho);
        if (!arquivo.isFile()) {
            throw new IllegalArgumentException("Arquivo de certificado não encontrado: " + caminho);
        }
        String tipoEfetivo = tipo == null || tipo.isBlank() ? "PKCS12" : tipo;
        KeyStore keyStore = KeyStore.getInstance(tipoEfetivo);
        char[] password = senha == null ? new char[0] : senha.toCharArray();
        try (InputStream input = new FileInputStream(arquivo)) {
            keyStore.load(input, password);
        }
        return keyStore;
    }
}

