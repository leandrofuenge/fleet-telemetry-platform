package com.telemetria.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.application.service.BackpressureMonitorService;

@RestController
@RequestMapping("/api/v1/admin/backpressure")
public class BackpressureAdminController {

    private static final Logger log = LoggerFactory.getLogger(BackpressureAdminController.class);
    private final BackpressureMonitorService backpressureMonitor;

    public BackpressureAdminController(BackpressureMonitorService backpressureMonitor) {
        this.backpressureMonitor = backpressureMonitor;
        log.info("✅ BackpressureAdminController inicializado");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.debug("📊 Requisição de status do backpressure recebida");
        
        long startTime = System.currentTimeMillis();
        
        int lag = backpressureMonitor.calcularLag();
        double taxa = backpressureMonitor.calcularTaxaProcessamento();
        double cpu = backpressureMonitor.getCpuUsage();
        double memory = backpressureMonitor.getMemoryUsage();
        boolean ativo = backpressureMonitor.isBackpressureAtivo();

        log.debug("📈 Status atual - Lag: {}, Taxa: {:.2f} msg/s, CPU: {:.1f}%, Memória: {:.1f}%, Ativo: {}", 
                 lag, taxa, cpu, memory, ativo);

        Map<String, Object> status = new HashMap<>();
        status.put("lag", lag);
        status.put("taxaProcessamento", taxa);
        status.put("cpuUsage", cpu);
        status.put("memoryUsage", memory);
        status.put("backpressureAtivo", ativo);
        status.put("mensagensRecebidas", backpressureMonitor.getMensagensRecebidas());
        status.put("mensagensProcessadas", backpressureMonitor.getMensagensProcessadas());
        status.put("tempoTotalProcessamento", backpressureMonitor.getTempoTotalProcessamento());

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("✅ Status retornado em {}ms", elapsed);

        return ResponseEntity.ok(status);
    }

    @PostMapping("/config")
    public ResponseEntity<String> configurar(
            @RequestParam(required = false) Integer lagThreshold,
            @RequestParam(required = false) Integer cpuThreshold,
            @RequestParam(required = false) Integer memoryThreshold,
            @RequestParam(required = false) Integer pauseDuration) {

        log.info("⚙️ Requisição para atualizar configurações de backpressure recebida");
        
        // Log dos parâmetros recebidos
        if (lagThreshold != null) {
            log.debug("📝 LagThreshold: {} (anterior: {})", lagThreshold, backpressureMonitor.getLagThreshold());
            backpressureMonitor.setLagThreshold(lagThreshold);
        }
        if (cpuThreshold != null) {
            log.debug("📝 CpuThreshold: {} (anterior: {})", cpuThreshold, backpressureMonitor.getCpuThreshold());
            backpressureMonitor.setCpuThreshold(cpuThreshold);
        }
        if (memoryThreshold != null) {
            log.debug("📝 MemoryThreshold: {} (anterior: {})", memoryThreshold, backpressureMonitor.getMemoryThreshold());
            backpressureMonitor.setMemoryThreshold(memoryThreshold);
        }
        if (pauseDuration != null) {
            log.debug("📝 PauseDuration: {}ms (anterior: {}ms)", pauseDuration, backpressureMonitor.getPauseDurationMs());
            backpressureMonitor.setPauseDurationMs(pauseDuration);
        }

        log.info("✅ Configurações de backpressure atualizadas com sucesso");
        log.debug("📊 Configurações atuais - Lag: {}, CPU: {}, Memória: {}, Pause: {}ms",
                 backpressureMonitor.getLagThreshold(),
                 backpressureMonitor.getCpuThreshold(),
                 backpressureMonitor.getMemoryThreshold(),
                 backpressureMonitor.getPauseDurationMs());

        return ResponseEntity.ok("Configurações atualizadas");
    }
}