package com.app.telemetria.controller;

import com.app.telemetria.service.CacheWarmingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/cache")
public class CacheAdminController {

    private final CacheWarmingService cacheWarmingService;

    public CacheAdminController(CacheWarmingService cacheWarmingService) {
        this.cacheWarmingService = cacheWarmingService;
    }

    @PostMapping("/warm")
    public ResponseEntity<Map<String, Object>> warmUpCache() {
        System.out.println("👨‍💼 Warming manual solicitado");
        
        long inicio = System.currentTimeMillis();
        cacheWarmingService.warmUpAllCaches();
        long fim = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache warming executado");
        response.put("timeMs", fim - inicio);
        response.put("timestamp", System.currentTimeMillis());

        System.out.println("✅ Warming manual concluído em " + (fim - inicio) + "ms");
        return ResponseEntity.ok(response);
    }
}