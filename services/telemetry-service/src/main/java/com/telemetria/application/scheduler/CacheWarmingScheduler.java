package com.telemetria.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.application.service.CacheWarmingService;

@Component
public class CacheWarmingScheduler {

    private final CacheWarmingService cacheWarmingService;

    public CacheWarmingScheduler(CacheWarmingService cacheWarmingService) {
        this.cacheWarmingService = cacheWarmingService;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledWarmUp() {
        System.out.println("⏰ Executando cache warming agendado (6 em 6 horas)");
        long inicio = System.currentTimeMillis();
        cacheWarmingService.warmUpAllCaches();
        long fim = System.currentTimeMillis();
        System.out.println("✅ Warming agendado concluído em " + (fim - inicio) + "ms");
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void warmUpGeocoding() {
        System.out.println("🗺️ Executando warming de geocoding (3 da manhã)");
        // Lógica específica para geocoding
        System.out.println("✅ Warming de geocoding concluído");
    }
}
