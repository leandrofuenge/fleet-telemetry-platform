package com.telemetria.integration.nfe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configurações compartilhadas de certificado utilizadas pela integração NF-e. */
@Component
@ConfigurationProperties(prefix = "sefaz")
public class SefazProperties {

    private Certificado certificado = new Certificado();

    public Certificado getCertificado() {
        return certificado;
    }

    public void setCertificado(Certificado certificado) {
        this.certificado = certificado;
    }

    public static class Certificado {
        private String arquivo;
        private String senha;
        private String senhaChave;
        private String tipo = "PKCS12";

        public String getArquivo() { return arquivo; }
        public void setArquivo(String arquivo) { this.arquivo = arquivo; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
        public String getSenhaChave() { return senhaChave; }
        public void setSenhaChave(String senhaChave) { this.senhaChave = senhaChave; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }
}

