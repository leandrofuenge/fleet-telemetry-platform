package com.app.telemetria.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.telemetria.application.service.CacheWarmingService;

@RestController
@RequestMapping("/api/v1/admin/cache")
public class CacheAdminController {

    private static final Logger log = LoggerFactory.getLogger(CacheAdminController.class);
    private final CacheWarmingService cacheWarmingService;

    public CacheAdminController(CacheWarmingService cacheWarmingService) {
        this.cacheWarmingService = cacheWarmingService;
        log.info("✅ CacheAdminController inicializado");
    }

    @PostMapping("/warm")
    public ResponseEntity<Map<String, Object>> warmUpCache() {
        log.info("👨‍💼 Requisição de warming manual de cache recebida");
        
        long inicio = System.currentTimeMillis();
        log.debug("🔄 Iniciando processo de warming manual...");

        try {
            cacheWarmingService.warmUpAllCaches();
            
            long fim = System.currentTimeMillis();
            long duracao = fim - inicio;
            
            log.info("✅ Warming manual concluído em {}ms", duracao);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache warming executado com sucesso");
            response.put("timeMs", duracao);
            response.put("timestamp", System.currentTimeMillis());

            log.debug("📤 Resposta: {}", response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro durante warming manual: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao executar cache warming: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}