package com.telemetria.integration.senatran.serpro.infrastructure.resilience;
import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SerproResiliencePolicyTests {
    @Test
    void abreCircuitoAposLimiteDeFalhas() {
        SerproProperties properties = new SerproProperties();
        properties.getSerpro().setCircuitFailureThreshold(1);
        SerproResiliencePolicy policy = new SerproResiliencePolicy(properties);
        assertThat(policy.acquire()).isEqualTo(SerproResiliencePolicy.Decision.ALLOWED);
        policy.failure();
        assertThat(policy.acquire()).isEqualTo(SerproResiliencePolicy.Decision.CIRCUIT_OPEN);
    }

    @Test
    void limitaQuantidadeDeConsultasNaJanela() {
        SerproProperties properties = new SerproProperties();
        properties.getSerpro().setRequestsPerMinute(1);
        SerproResiliencePolicy policy = new SerproResiliencePolicy(properties);
        assertThat(policy.acquire()).isEqualTo(SerproResiliencePolicy.Decision.ALLOWED);
        assertThat(policy.acquire()).isEqualTo(SerproResiliencePolicy.Decision.RATE_LIMITED);
    }
}
