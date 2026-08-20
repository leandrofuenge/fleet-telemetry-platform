package com.telemetria.integration.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteAmbiente;

@Component
@ConfigurationProperties(prefix = "sefaz")
public class SefazProperties {

    private String estado = "MT";
    private Cte cte = new Cte();
    private Certificado certificado = new Certificado();
    private Tls tls = new Tls();

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Alias para suporte transparente caso a propriedade venha como 'sefaz.uf'
    public String getUf() {
        return estado;
    }

    public void setUf(String uf) {
        this.estado = uf;
    }

    public Cte getCte() {
        return cte;
    }

    public void setCte(Cte cte) {
        this.cte = cte;
    }

    public Certificado getCertificado() {
        return certificado;
    }

    public void setCertificado(Certificado certificado) {
        this.certificado = certificado;
    }

    public Tls getTls() { 
        return tls; 
    }

    public void setTls(Tls tls) { 
        this.tls = tls; 
    }

    public static class Cte {
        private String ambiente = "homologacao";
        private String versao = "4.00";
        private Endpoints endpoints = new Endpoints();
        private Operations operations = new Operations();

        public String getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(String ambiente) {
            this.ambiente = ambiente;
        }

        public CteAmbiente ambienteCte() {
            return CteAmbiente.from(ambiente);
        }

        public String getVersao() {
            return versao;
        }

        public void setVersao(String versao) {
            this.versao = versao;
        }

        public Endpoints getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Endpoints endpoints) {
            this.endpoints = endpoints;
        }

        public Operations getOperations() { 
            return operations; 
        }

        public void setOperations(Operations operations) { 
            this.operations = operations; 
        }
    }

    public static class Operations {
        private boolean authorizationEnabled;
        private boolean cancellationEnabled;
        private boolean authorizedFiscalTestData;

        public boolean isAuthorizationEnabled() { 
            return authorizationEnabled; 
        }

        public void setAuthorizationEnabled(boolean authorizationEnabled) {
            this.authorizationEnabled = authorizationEnabled;
        }

        public boolean isCancellationEnabled() { 
            return cancellationEnabled; 
        }

        public void setCancellationEnabled(boolean cancellationEnabled) {
            this.cancellationEnabled = cancellationEnabled;
        }

        public boolean isAuthorizedFiscalTestData() { 
            return authorizedFiscalTestData; 
        }

        public void setAuthorizedFiscalTestData(boolean authorizedFiscalTestData) {
            this.authorizedFiscalTestData = authorizedFiscalTestData;
        }
    }

    public static class Endpoints {
        private URI autorizacao;
        private URI consulta;
        private URI evento;
        private URI status;

        public URI getAutorizacao() { return autorizacao; }
        public void setAutorizacao(URI autorizacao) { this.autorizacao = autorizacao; }
        public URI getConsulta() { return consulta; }
        public void setConsulta(URI consulta) { this.consulta = consulta; }
        public URI getEvento() { return evento; }
        public void setEvento(URI evento) { this.evento = evento; }
        public URI getStatus() { return status; }
        public void setStatus(URI status) { this.status = status; }
    }

    public static class Certificado {
        private String arquivo;
        private String senha;
        private String senhaChave;
        private String tipo = "PKCS12";

        public String getArquivo() {
            return arquivo;
        }

        public void setArquivo(String arquivo) {
            this.arquivo = arquivo;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        public String getSenhaChave() { return senhaChave; }
        public void setSenhaChave(String senhaChave) { this.senhaChave = senhaChave; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }

    public static class Tls {
        private String protocolo = "TLSv1.2";
        private Truststore truststore = new Truststore();

        public String getProtocolo() { return protocolo; }
        public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
        public Truststore getTruststore() { return truststore; }
        public void setTruststore(Truststore truststore) { this.truststore = truststore; }
    }

    public static class Truststore {
        private String arquivo;
        private String senha;
        private String tipo = "PKCS12";

        public String getArquivo() { return arquivo; }
        public void setArquivo(String arquivo) { this.arquivo = arquivo; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }
}
