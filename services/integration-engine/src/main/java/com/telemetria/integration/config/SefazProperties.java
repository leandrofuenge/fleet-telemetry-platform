package com.telemetria.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sefaz")
public class SefazProperties {

    private Cte cte = new Cte();
    private Certificado certificado = new Certificado();

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

    public static class Cte {
        private String ambiente = "homologacao";
        private String versao = "4.00";
        private StatusServico statusServico = new StatusServico();

        public String getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(String ambiente) {
            this.ambiente = ambiente;
        }

        public String getVersao() {
            return versao;
        }

        public void setVersao(String versao) {
            this.versao = versao;
        }

        public StatusServico getStatusServico() {
            return statusServico;
        }

        public void setStatusServico(StatusServico statusServico) {
            this.statusServico = statusServico;
        }
    }

    public static class StatusServico {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Certificado {
        private String arquivo;
        private String senha;

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
    }
}
