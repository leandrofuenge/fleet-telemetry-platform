package com.telemetria.infrastructure.integration.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.service")
public class SerproIntegrationProperties {

    private String url = "http://localhost:9060";
    private String serproApiKey = "";

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSerproApiKey() { return serproApiKey; }
    public void setSerproApiKey(String serproApiKey) { this.serproApiKey = serproApiKey; }
}
