package com.telemetria.integration.support.observability;

import org.apache.camel.Exchange;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CamelFlowLoggingNotifier extends EventNotifierSupport {

    private static final String START_NANOS = CamelFlowLoggingNotifier.class.getName() + ".startNanos";
    private static final Logger log = LoggerFactory.getLogger(CamelFlowLoggingNotifier.class);

    @Override
    public void notify(CamelEvent event) {
        if (event instanceof CamelEvent.ExchangeCreatedEvent created) {
            Exchange exchange = created.getExchange();
            exchange.setProperty(START_NANOS, System.nanoTime());
            log.info("CAMEL IN  route={} exchangeId={} from={}", routeId(exchange),
                    exchange.getExchangeId(), endpoint(exchange));
        } else if (event instanceof CamelEvent.ExchangeCompletedEvent completed) {
            Exchange exchange = completed.getExchange();
            log.info("CAMEL OUT route={} exchangeId={} status=SUCCESS durationMs={}", routeId(exchange),
                    exchange.getExchangeId(), durationMs(exchange));
        } else if (event instanceof CamelEvent.ExchangeFailedEvent failed) {
            Exchange exchange = failed.getExchange();
            Throwable cause = exchange.getException();
            if (cause == null) cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
            log.error("CAMEL OUT route={} exchangeId={} status=FAILED durationMs={} errorType={} message={}",
                    routeId(exchange), exchange.getExchangeId(), durationMs(exchange),
                    cause != null ? cause.getClass().getSimpleName() : "Unknown",
                    cause != null ? cause.getMessage() : "Falha sem detalhe");
        }
    }

    @Override
    public boolean isEnabled(CamelEvent event) {
        return event instanceof CamelEvent.ExchangeCreatedEvent
                || event instanceof CamelEvent.ExchangeCompletedEvent
                || event instanceof CamelEvent.ExchangeFailedEvent;
    }

    private String routeId(Exchange exchange) {
        return exchange.getFromRouteId() != null ? exchange.getFromRouteId() : "unknown";
    }

    private String endpoint(Exchange exchange) {
        return exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown";
    }

    private long durationMs(Exchange exchange) {
        Long start = exchange.getProperty(START_NANOS, Long.class);
        return start == null ? -1 : (System.nanoTime() - start) / 1_000_000;
    }
}
