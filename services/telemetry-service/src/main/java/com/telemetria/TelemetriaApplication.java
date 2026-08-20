package com.telemetria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.telemetria.infrastructure.integration.engine.SerproIntegrationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SerproIntegrationProperties.class)
@EnableCaching
@EnableRetry
@EnableScheduling
public class TelemetriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelemetriaApplication.class, args);
	}
}
