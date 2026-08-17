package com.telemetria.domain.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.domain.entity.Telemetria;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class TelemetriaBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaBatchProcessor.class);

    private final TelemetriaService telemetriaService;
    private final ExecutorService loteExecutor;

    public TelemetriaBatchProcessor(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
        // Criar um pool fixo de 10 threads evita sobrecarga sob fluxos intensos de dados
        this.loteExecutor = Executors.newFixedThreadPool(10); 
    }

    /**
     * Processamento assíncrono em lote SEM travar conexões de banco na thread mãe.
     */
    public CompletableFuture<List<String>> processarMultiplasTelemetrias(List<Telemetria> telemetrias) {
        if (telemetrias == null || telemetrias.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        long inicio = System.currentTimeMillis();
        log.info("🔄 Processando lote de {} telemetrias assincronamente", telemetrias.size());

        // Agora o map consegue inferir os tipos perfeitamente!
        List<CompletableFuture<String>> futures = telemetrias.stream()
                .map(t -> telemetriaService.processarTelemetria(t, loteExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    log.info("✅ Lote de {} telemetrias processado em {}ms", telemetrias.size(), (System.currentTimeMillis() - inicio));
                    return futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                });
    }

    /**
     * Processamento reativo integrado com WebFlux (Project Reactor)
     */
    public Flux<String> processarMultiplasTelemetriasReativo(List<Telemetria> telemetrias) {
        if (telemetrias == null || telemetrias.isEmpty()) {
            return Flux.empty();
        }

        log.info("🔄 Processando reativamente lote de {} telemetrias", telemetrias.size());

        return Flux.fromIterable(telemetrias)
                .flatMap(telemetria -> 
                    Mono.fromFuture(telemetriaService.processarTelemetria(telemetria, loteExecutor))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorReturn("Erro no processamento")
                );
    }
}