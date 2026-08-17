package com.telemetria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Permite todos os endpoints
                        .allowedOrigins(
                            "http://localhost:8080", // Porta do seu Frontend PHP
                            "http://127.0.0.1:8080", // IP alternativo do localhost
                            "http://localhost:5173"  // Mantive a do Vite por segurança
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*") // Permite todos os headers (importante para o JWT)
                        .allowCredentials(true); // Permite envio de cookies/auth se necessário
            }
        };
    }
}