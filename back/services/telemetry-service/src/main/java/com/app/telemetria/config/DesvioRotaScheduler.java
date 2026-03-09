package com.app.telemetria.config;

import com.app.telemetria.service.DetectorDesvioRotaService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DesvioRotaScheduler {
    
    private final DetectorDesvioRotaService detectorService;
    
    public DesvioRotaScheduler(DetectorDesvioRotaService detectorService) {
        this.detectorService = detectorService;
    }
    
    @Scheduled(fixedDelay = 30000) // A cada 30 segundos
    public void verificarDesvios() {
        detectorService.verificarDesviosAtivos();
    }
}