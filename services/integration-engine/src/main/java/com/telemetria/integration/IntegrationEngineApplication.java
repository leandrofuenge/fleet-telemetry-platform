package com.telemetria.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IntegrationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationEngineApplication.class, args);
    }
}
