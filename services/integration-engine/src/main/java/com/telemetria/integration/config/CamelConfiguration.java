package com.telemetria.integration.config;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.telemetria.integration.support.observability.CamelFlowLoggingNotifier;

@Configuration
public class CamelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public CamelFlowLoggingNotifier camelFlowLoggingNotifier() {
        return new CamelFlowLoggingNotifier();
    }

    @Bean
    public CamelContextConfiguration contextConfiguration(CamelFlowLoggingNotifier flowLoggingNotifier) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                log.info("Inicializando configuracoes globais do Apache Camel Context: {}", camelContext.getName());

                // Ativa cache de stream para permitir leitura multipla de payloads XML/JSON
                camelContext.setStreamCaching(true);

                // Ativa MDC (Mapped Diagnostic Context) para correlacao de logs
                camelContext.setUseMDCLogging(true);

                // Ativa rastreamento de historico de mensagens
                camelContext.setMessageHistory(true);

                // Registra início, término, duração e falha de todas as rotas sem expor payloads.
                camelContext.getManagementStrategy().addEventNotifier(flowLoggingNotifier);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                log.info("Apache Camel Context [{}] iniciado com {} rotas ativas.",
                        camelContext.getName(), camelContext.getRoutesSize());
            }
        };
    }
}
