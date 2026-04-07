// src/main/java/com/telemetria/application/scheduler/ETAScheduler.java
package com.telemetria.application.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.service.ETACalculationService;

@Component
public class ETAScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ETAScheduler.class);
    
    @Autowired
    private ETACalculationService etaCalculationService;
    
    /**
     * RN-VIA-003: Recalcula ETA a cada 5 minutos
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos em milissegundos
    public void recalcularETAPeriodico() {
        log.info("⏰ [ETA SCHEDULER] Executando recálculo periódico de ETA (5 minutos)");
        
        try {
            int totalRecalculados = etaCalculationService.recalcularETAEmLote();
            log.info("✅ [ETA SCHEDULER] ETA recalculado para {} viagens", totalRecalculados);
        } catch (Exception e) {
            log.error("❌ [ETA SCHEDULER] Erro ao recalcular ETA: {}", e.getMessage(), e);
        }
    }
}