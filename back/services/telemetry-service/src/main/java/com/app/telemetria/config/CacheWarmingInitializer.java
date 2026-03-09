package com.app.telemetria.config;

import com.app.telemetria.service.CacheWarmingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.cache.warming.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmingInitializer implements CommandLineRunner {

    private final CacheWarmingService cacheWarmingService;

    public CacheWarmingInitializer(CacheWarmingService cacheWarmingService) {
        this.cacheWarmingService = cacheWarmingService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔥 Iniciando Cache Warming na inicialização...");
        
        try {
            cacheWarmingService.warmUpAllCaches();
            System.out.println("✅ Cache Warming finalizado com sucesso");
        } catch (Exception e) {
            System.err.println("❌ Erro no Cache Warming: " + e.getMessage());
            e.printStackTrace();
            // Não falha a aplicação se o cache warming der erro
        }
    }
}