package com.telemetria.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.service.TenantService;

@Component
public class TenantTrialScheduler {
    private final TenantService tenantService;

    public TenantTrialScheduler(TenantService tenantService) { this.tenantService = tenantService; }

    @Scheduled(cron = "${tenant.trial.expiration-cron:0 0 * * * *}")
    public void expirarTrials() { tenantService.expirarTrials(); }
}
