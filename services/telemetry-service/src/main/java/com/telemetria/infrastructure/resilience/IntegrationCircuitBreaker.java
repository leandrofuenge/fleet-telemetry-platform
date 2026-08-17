package com.telemetria.infrastructure.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Circuit breaker leve para impedir que indisponibilidade externa trave a ingestão. */
@Component
public class IntegrationCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCircuitBreaker.class);

    private final Map<String, CircuitState> states = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final Duration openDuration;

    public IntegrationCircuitBreaker(
            @Value("${telemetria.integrations.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${telemetria.integrations.circuit-breaker.open-seconds:30}") long openSeconds) {
        this.failureThreshold = failureThreshold;
        this.openDuration = Duration.ofSeconds(openSeconds);
    }

    public <T> T execute(String integration, Supplier<T> action, Supplier<T> fallback) {
        CircuitState state = states.computeIfAbsent(integration, ignored -> new CircuitState());
        if (!state.allowRequest(openDuration)) {
            log.warn("Circuit breaker aberto para {}", integration);
            return fallback.get();
        }

        try {
            T result = action.get();
            state.recordSuccess();
            return result;
        } catch (RuntimeException e) {
            boolean opened = state.recordFailure(failureThreshold);
            log.warn("Integração {} falhou{}: {}", integration, opened ? " e abriu o circuito" : "", e.getMessage());
            return fallback.get();
        }
    }

    public void run(String integration, Runnable action) {
        execute(integration, () -> {
            action.run();
            return Boolean.TRUE;
        }, () -> Boolean.FALSE);
    }

    private static final class CircuitState {
        private int failures;
        private Instant openedAt;
        private boolean halfOpenRequestInFlight;

        synchronized boolean allowRequest(Duration openDuration) {
            if (openedAt == null) return true;
            if (Instant.now().isBefore(openedAt.plus(openDuration))) return false;
            if (halfOpenRequestInFlight) return false;
            halfOpenRequestInFlight = true;
            return true;
        }

        synchronized void recordSuccess() {
            failures = 0;
            openedAt = null;
            halfOpenRequestInFlight = false;
        }

        synchronized boolean recordFailure(int threshold) {
            failures++;
            halfOpenRequestInFlight = false;
            if (failures >= threshold) {
                openedAt = Instant.now();
                return true;
            }
            return false;
        }
    }
}
