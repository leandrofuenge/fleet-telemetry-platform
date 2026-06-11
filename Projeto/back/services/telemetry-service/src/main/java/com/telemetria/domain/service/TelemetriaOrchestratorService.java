package com.telemetria.domain.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class TelemetriaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaOrchestratorService.class);

    private final ViagemRepository viagemRepository;
    private final TelemetriaRegrasService regrasService;

    public TelemetriaOrchestratorService(ViagemRepository viagemRepository, TelemetriaRegrasService regrasService) {
        this.viagemRepository = viagemRepository;
        this.regrasService = regrasService;
    }

    /**
     * Processa uma única telemetria de forma assíncrona real (Non-blocking pipeline).
     */
    @Async("telemetriaTaskExecutor")
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculoId() == null) {
            log.warn("⚠️ Telemetria inválida descartada no início do pipeline");
            return CompletableFuture.completedFuture("Telemetria inválida");
        }

        return CompletableFuture.supplyAsync(() -> {
            long inicio = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();
            Long veiculoId = telemetria.getVeiculoId();

            log.info("🔄 [Thread: {}] Iniciando pipeline de alertas para veículo {}", threadName, veiculoId);

            try {
                // Recupera o contexto da viagem ativa de forma limpa (Sem lock transacional)
                Viagem viagemAtiva = viagemRepository.findByVeiculoIdAndStatus(veiculoId, "EM_ANDAMENTO")
                        .orElse(null);

                // Execução do motor de regras em memória
                regrasService.verificarExcessoVelocidade(telemetria);
                regrasService.verificarVelocidadeBaixa(telemetria, viagemAtiva);
                regrasService.verificarNivelCombustivel(telemetria, viagemAtiva);
                regrasService.verificarGpsSemSinal(veiculoId, telemetria);

                if (viagemAtiva != null) {
                    regrasService.verificarTempoDirecao(viagemAtiva, telemetria);
                    regrasService.verificarAtrasoViagemInteligente(viagemAtiva, telemetria);
                }

                // Gatilhos de auto-resolução de alertas normalizados
                regrasService.resolverAlertaGpsSeNecessario(veiculoId);
                regrasService.resolverAlertaCombustivelSeNecessario(veiculoId, telemetria.getNivelCombustivel());

                long fim = System.currentTimeMillis();
                log.info("✅ [Thread: {}] Pipeline concluído in {}ms para veículo {}", threadName, (fim - inicio), veiculoId);

                return "Alertas processados com sucesso para veículo " + veiculoId;

            } catch (Exception e) {
                log.error("❌ [Thread: {}] Falha crítica no processamento do veículo {}: {}", 
                         threadName, veiculoId, e.getMessage(), e);
                throw new RuntimeException("Erro ao processar telemetria do veículo " + veiculoId, e);
            }
        });
    }

    /**
     * Processa uma lista de telemetrias em paralelo (Fan-Out distribuído).
     */
    public CompletableFuture<List<String>> processarMultiplasTelemetrias(List<Telemetria> telemetrias) {
        if (telemetrias == null || telemetrias.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("📦 Recebido lote de {} telemetrias para processamento paralelo", telemetrias.size());

        List<CompletableFuture<String>> futures = telemetrias.stream()
                .map(this::processarTelemetria)
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> futures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception e) {
                        return "Falha no item: " + e.getMessage();
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
    }
}