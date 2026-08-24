package com.telemetria.integration.sefaz.cte.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.validation.CteFiscalOperationGuard;

@Component
public class CteCertificateHealthIndicator implements HealthIndicator {

    private final CteFiscalOperationGuard operationGuard;

    public CteCertificateHealthIndicator(CteFiscalOperationGuard operationGuard) {
        this.operationGuard = operationGuard;
    }

    @Override
    public Health health() {
        try {
            operationGuard.exigirAutorizacaoPermitida();
            return Health.up().withDetail("status", "Certificado A1 válido e operacional.").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("status", "Certificado A1 ou operação bloqueada.").build();
        }
    }
}
