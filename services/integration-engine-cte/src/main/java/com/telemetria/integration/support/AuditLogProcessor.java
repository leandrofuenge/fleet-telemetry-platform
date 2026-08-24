package com.telemetria.integration.support;

import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("auditLogProcessor")
public class AuditLogProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(AuditLogProcessor.class);
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_START_TIME = "X-Start-Time";

    @Override
    public void process(Exchange exchange) {
        String correlationId = exchange.getIn().getHeader(HEADER_CORRELATION_ID, String.class);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            exchange.getIn().setHeader(HEADER_CORRELATION_ID, correlationId);
        }

        Long startTime = exchange.getProperty(HEADER_START_TIME, Long.class);
        if (startTime == null) {
            exchange.setProperty(HEADER_START_TIME, System.currentTimeMillis());
            log.info("[Camel Audit] INICIO | ExchangeId: {} | CorrelationId: {} | Endpoint: {}",
                    exchange.getExchangeId(), correlationId, exchange.getFromEndpoint());
        } else {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[Camel Audit] FIM | ExchangeId: {} | CorrelationId: {} | Tempo: {}ms",
                    exchange.getExchangeId(), correlationId, duration);
        }
    }
}
