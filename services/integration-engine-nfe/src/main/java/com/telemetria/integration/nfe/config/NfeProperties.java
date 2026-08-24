package com.telemetria.integration.nfe.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuração específica da NF-e, separada do fluxo CT-e. */
@Component
@ConfigurationProperties(prefix = "sefaz.nfe")
public class NfeProperties {
    private String ambiente = "2";
    private String codigoUf = "51";
    private int timeoutMillis = 30000;
    private int maxXmlBytes = 1048576;
    private Endpoints endpoints = new Endpoints();
    private Operations operations = new Operations();

    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public String getCodigoUf() { return codigoUf; }
    public void setCodigoUf(String codigoUf) { this.codigoUf = codigoUf; }
    public int getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    public int getMaxXmlBytes() { return maxXmlBytes; }
    public void setMaxXmlBytes(int maxXmlBytes) { this.maxXmlBytes = maxXmlBytes; }
    public Endpoints getEndpoints() { return endpoints; }
    public void setEndpoints(Endpoints endpoints) { this.endpoints = endpoints; }
    public Operations getOperations() { return operations; }
    public void setOperations(Operations operations) { this.operations = operations; }

    public static class Endpoints {
        private URI autorizacao;
        private URI retAutorizacao;
        private URI consulta;
        private URI evento;
        private URI inutilizacao;
        private URI statusServico;
        private URI distribuicaoDfe;
        public URI getAutorizacao() { return autorizacao; }
        public void setAutorizacao(URI value) { autorizacao = value; }
        public URI getRetAutorizacao() { return retAutorizacao; }
        public void setRetAutorizacao(URI value) { retAutorizacao = value; }
        public URI getConsulta() { return consulta; }
        public void setConsulta(URI value) { consulta = value; }
        public URI getEvento() { return evento; }
        public void setEvento(URI value) { evento = value; }
        public URI getInutilizacao() { return inutilizacao; }
        public void setInutilizacao(URI value) { inutilizacao = value; }
        public URI getStatusServico() { return statusServico; }
        public void setStatusServico(URI value) { statusServico = value; }
        public URI getDistribuicaoDfe() { return distribuicaoDfe; }
        public void setDistribuicaoDfe(URI value) { distribuicaoDfe = value; }
    }

    public static class Operations {
        private boolean authorizationEnabled;
        private boolean eventEnabled;
        private boolean inutilizationEnabled;
        private boolean authorizedFiscalTestData;
        public boolean isAuthorizationEnabled() { return authorizationEnabled; }
        public void setAuthorizationEnabled(boolean value) { authorizationEnabled = value; }
        public boolean isEventEnabled() { return eventEnabled; }
        public void setEventEnabled(boolean value) { eventEnabled = value; }
        public boolean isInutilizationEnabled() { return inutilizationEnabled; }
        public void setInutilizationEnabled(boolean value) { inutilizationEnabled = value; }
        public boolean isAuthorizedFiscalTestData() { return authorizedFiscalTestData; }
        public void setAuthorizedFiscalTestData(boolean value) { authorizedFiscalTestData = value; }
    }
}
