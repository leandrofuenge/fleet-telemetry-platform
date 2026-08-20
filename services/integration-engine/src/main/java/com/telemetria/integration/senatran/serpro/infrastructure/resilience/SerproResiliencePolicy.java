package com.telemetria.integration.senatran.serpro.infrastructure.resilience;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;

/** Circuit breaker e limitador de taxa locais, sem dependência do fornecedor. */
@Component
public class SerproResiliencePolicy {
    private final SerproProperties properties;
    private final Clock clock;
    private long windowStart;
    private int requestsInWindow;
    private int consecutiveFailures;
    private long circuitOpenedAt;

    @Autowired
    public SerproResiliencePolicy(SerproProperties properties) { this(properties, Clock.systemUTC()); }
    SerproResiliencePolicy(SerproProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.windowStart = clock.millis();
    }

    public synchronized Decision acquire() {
        long now = clock.millis();
        if (circuitOpenedAt > 0) {
            long openFor = properties.getSerpro().getCircuitOpenDuration().toMillis();
            if (now - circuitOpenedAt < openFor) return Decision.CIRCUIT_OPEN;
            circuitOpenedAt = 0;
            consecutiveFailures = 0;
        }
        if (now - windowStart >= 60_000) {
            windowStart = now;
            requestsInWindow = 0;
        }
        if (requestsInWindow >= properties.getSerpro().getRequestsPerMinute()) return Decision.RATE_LIMITED;
        requestsInWindow++;
        return Decision.ALLOWED;
    }

    public synchronized void success() { consecutiveFailures = 0; }

    public synchronized void failure() {
        consecutiveFailures++;
        if (consecutiveFailures >= properties.getSerpro().getCircuitFailureThreshold()) {
            circuitOpenedAt = clock.millis();
        }
    }

    public enum Decision { ALLOWED, RATE_LIMITED, CIRCUIT_OPEN }
}
