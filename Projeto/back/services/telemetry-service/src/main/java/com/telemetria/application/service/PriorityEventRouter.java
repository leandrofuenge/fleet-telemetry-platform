package com.telemetria.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.service.AlertaService;

import jakarta.annotation.PostConstruct;

@Service
public class PriorityEventRouter {

    private static final Logger log = LoggerFactory.getLogger(PriorityEventRouter.class);

    private final Executor criticalExecutor;
    private final Executor highExecutor;
    private final Executor normalExecutor;
    private final ScheduledExecutorService batchScheduler;
    private final AlertaService alertaService;

    private final List<Telemetria> normalBatch = new ArrayList<>();
    private final Object batchLock = new Object();

    @Value("${priority.normal.batch.interval.ms:1000}")
    private int batchIntervalMs;

    @Value("${priority.normal.batch.max.size:100}")
    private int batchMaxSize;

    @Value("${priority.critical.queue.max:1000}")
    private int criticalQueueMax;

    private final AtomicBoolean criticalQueueOverloaded = new AtomicBoolean(false);
    private ThreadPoolTaskExecutor criticalExecutorImpl;

    @Autowired
    public PriorityEventRouter(@Qualifier("criticalTaskExecutor") Executor criticalExecutor,
                               @Qualifier("highTaskExecutor") Executor highExecutor,
                               @Qualifier("normalTaskExecutor") Executor normalExecutor,
                               @Qualifier("batchScheduler") ScheduledExecutorService batchScheduler,
                               AlertaService alertaService) {
        this.criticalExecutor = criticalExecutor;
        this.highExecutor = highExecutor;
        this.normalExecutor = normalExecutor;
        this.batchScheduler = batchScheduler;
        this.alertaService = alertaService;
    }

    @PostConstruct
    public void init() {
        if (criticalExecutor instanceof ThreadPoolTaskExecutor) {
            this.criticalExecutorImpl = (ThreadPoolTaskExecutor) criticalExecutor;
        } else {
            log.warn("Critical executor is not a ThreadPoolTaskExecutor, cannot monitor queue size");
        }
        batchScheduler.scheduleAtFixedRate(this::flushNormalBatch, batchIntervalMs, batchIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void route(Telemetria telemetria, JsonNode json) {
        Priority priority = determinePriority(telemetria, json);
        log.debug("Routing telemetria {} with priority {}", telemetria.getId(), priority);

        switch (priority) {
            case CRITICAL:
                criticalExecutor.execute(() -> processCritical(telemetria));
                break;
            case HIGH:
                highExecutor.execute(() -> processHigh(telemetria));
                break;
            case NORMAL:
                if (isCriticalQueueOverloaded()) {
                    log.warn("Critical queue overloaded (size > {}), discarding NORMAL telemetria", criticalQueueMax);
                    // descarta a mensagem
                } else {
                    addToNormalBatch(telemetria);
                }
                break;
        }
    }

    private Priority determinePriority(Telemetria telemetria, JsonNode json) {
        // Eventos CRÍTICOS
        if (json.has("botao_panico") && json.get("botao_panico").asBoolean()) {
            return Priority.CRITICAL;
        }
        if (json.has("colisao_detectada") && json.get("colisao_detectada").asBoolean()) {
            return Priority.CRITICAL;
        }
        if (json.has("adulteracao_gps") && json.get("adulteracao_gps").asBoolean()) {
            return Priority.CRITICAL;
        }

        // Eventos ALTO
        if (json.has("fadiga_detectada") && json.get("fadiga_detectada").asBoolean()) {
            return Priority.HIGH;
        }
        if (json.has("temperatura_carga") && json.get("temperatura_carga").asDouble() > 30.0) {
            return Priority.HIGH;
        }
        if (json.has("pressao_pneus_json") && json.get("pressao_pneus_json").isArray()) {
            for (JsonNode pneu : json.get("pressao_pneus_json")) {
                if (pneu.has("psi") && pneu.get("psi").asDouble() < 80.0) {
                    return Priority.HIGH;
                }
            }
        }

        // Padrão NORMAL
        return Priority.NORMAL;
    }

    private boolean isCriticalQueueOverloaded() {
        if (criticalExecutorImpl == null) return false;
        BlockingQueue<Runnable> queue = criticalExecutorImpl.getThreadPoolExecutor().getQueue();
        int size = queue.size();
        boolean overloaded = size > criticalQueueMax;
        if (overloaded && !criticalQueueOverloaded.get()) {
            criticalQueueOverloaded.set(true);
            log.warn("Critical queue size {} exceeds max {}, activating backpressure", size, criticalQueueMax);
        } else if (!overloaded && criticalQueueOverloaded.get()) {
            criticalQueueOverloaded.set(false);
            log.info("Critical queue size {} back to normal, deactivating backpressure", size);
        }
        return overloaded;
    }

    private void addToNormalBatch(Telemetria telemetria) {
        synchronized (batchLock) {
            normalBatch.add(telemetria);
            if (normalBatch.size() >= batchMaxSize) {
                flushNormalBatch();
            }
        }
    }

    private void flushNormalBatch() {
        List<Telemetria> toProcess;
        synchronized (batchLock) {
            if (normalBatch.isEmpty()) return;
            toProcess = new ArrayList<>(normalBatch);
            normalBatch.clear();
        }
        log.debug("Flushing batch of {} normal telemetrias", toProcess.size());
        normalExecutor.execute(() -> alertaService.processarMultiplasTelemetrias(toProcess));
    }

    private void processCritical(Telemetria telemetria) {
        log.info("Processing CRITICAL telemetria {}", telemetria.getId());
        alertaService.processarTelemetria(telemetria);
    }

    private void processHigh(Telemetria telemetria) {
        log.info("Processing HIGH telemetria {}", telemetria.getId());
        alertaService.processarTelemetria(telemetria);
    }

    private enum Priority {
        CRITICAL, HIGH, NORMAL
    }
}