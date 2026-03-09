package com.app.telemetria.controller;

import com.app.telemetria.service.BackpressureMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/backpressure")
public class BackpressureAdminController {

    private final BackpressureMonitorService backpressureMonitor;

    public BackpressureAdminController(BackpressureMonitorService backpressureMonitor) {
        this.backpressureMonitor = backpressureMonitor;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lag", backpressureMonitor.calcularLag());
        status.put("taxaProcessamento", backpressureMonitor.calcularTaxaProcessamento());
        status.put("cpuUsage", backpressureMonitor.getCpuUsage());
        status.put("memoryUsage", backpressureMonitor.getMemoryUsage());
        status.put("backpressureAtivo", backpressureMonitor.isBackpressureAtivo());
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/config")
    public ResponseEntity<String> configurar(
            @RequestParam(required = false) Integer lagThreshold,
            @RequestParam(required = false) Integer cpuThreshold,
            @RequestParam(required = false) Integer memoryThreshold,
            @RequestParam(required = false) Integer pauseDuration) {
        
        if (lagThreshold != null) backpressureMonitor.setLagThreshold(lagThreshold);
        if (cpuThreshold != null) backpressureMonitor.setCpuThreshold(cpuThreshold);
        if (memoryThreshold != null) backpressureMonitor.setMemoryThreshold(memoryThreshold);
        if (pauseDuration != null) backpressureMonitor.setPauseDurationMs(pauseDuration);
        
        System.out.println("⚙️ Configurações de backpressure atualizadas");
        return ResponseEntity.ok("Configurações atualizadas");
    }
}