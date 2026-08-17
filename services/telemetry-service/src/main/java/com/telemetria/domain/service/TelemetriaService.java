package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

import jakarta.transaction.Transactional;

@Service
public class TelemetriaService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaService.class);

    private final TelemetriaRepository telemetriaRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;

    @Autowired
    public TelemetriaService(TelemetriaRepository telemetriaRepository, 
                             PosicaoAtualRepository posicaoAtualRepository) {
        this.telemetriaRepository = telemetriaRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
    }

    /**
     * 🟢 NOVO MÉTODO SINCRONIZADO COM O BATCH PROCESSOR
     * Orquestra o processamento de uma telemetria individual usando threads paralelas do lote.
     * NÃO possui @Transactional aqui para não prender conexões do pool globalmente.
     */
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria, ExecutorService executor) {
        if (telemetria == null || telemetria.getVeiculoId() == null) {
            log.warn("⚠️ Telemetria inválida recebida para processamento");
            return CompletableFuture.completedFuture("Telemetria inválida");
        }

        long inicio = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("🔄 [Thread: {}] Iniciando processamento de regras para veículo {}", threadName, telemetria.getVeiculoId());

        try {
            // 1. Salva a telemetria recebida de forma isolada
            salvar(telemetria);

            // 2. Cria a lista de validações que rodarão em paralelo no pool de threads do lote
            List<CompletableFuture<Void>> verificacoes = new ArrayList<>();
            
            verificacoes.add(CompletableFuture.runAsync(() -> verificarExcessoVelocidade(telemetria), executor));
            verificacoes.add(CompletableFuture.runAsync(() -> verificarNivelCombustivel(telemetria), executor));
            verificacoes.add(CompletableFuture.runAsync(() -> verificarGpsSemSinal(telemetria), executor));

            // Aguarda todas as verificações assíncronas terminarem
            CompletableFuture.allOf(verificacoes.toArray(new CompletableFuture[0])).join();

            // 3. Executa a resolução de alertas antigos e atualiza a posição atual (Métodos transacionais locais)
            resolverAlertas(telemetria);
            
            atualizarPosicaoAtual(
                telemetria.getVeiculoId(), 
                telemetria.getTenantId(), 
                telemetria.getVeiculoUuid(), // Certifique-se de que esses getters existam na sua Entity
                telemetria.getLatitude(), 
                telemetria.getLongitude(), 
                telemetria.getVelocidade(), 
                telemetria.getDirecao(), 
                telemetria.getIgnicao(), 
                telemetria.getDataHora()
            );

            log.info("✅ Alertas e posição processados em {}ms para o veículo {}", (System.currentTimeMillis() - inicio), telemetria.getVeiculoId());
            return CompletableFuture.completedFuture("Sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no processamento da telemetria do veículo {}: {}", telemetria.getVeiculoId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Salva telemetria no banco de dados com logs de debug
     */
    @Transactional
    public Telemetria salvar(Telemetria telemetria) {
        log.info("💾 [SERVICE] Iniciando salvamento da telemetria");
        log.debug("[SERVICE] Veículo ID: {}, Data/Hora: {}, Lat: {}, Lng: {}",
                telemetria.getVeiculoId(), telemetria.getDataHora(), telemetria.getLatitude(), telemetria.getLongitude());
        
        if (telemetria.getVeiculoId() == null) {
            log.error("❌ [SERVICE] ERRO: veiculoId é nulo!");
            throw new IllegalArgumentException("veiculoId não pode ser nulo");
        }
        if (telemetria.getTenantId() == null) {
            log.error("❌ [SERVICE] ERRO: tenantId é nulo!");
            throw new IllegalArgumentException("tenantId não pode ser nulo");
        }
        
        Telemetria saved = telemetriaRepository.save(telemetria);
        log.info("✅ [SERVICE] Telemetria salva com sucesso! ID: {}", saved.getId());
        
        return saved;
    }

    // =========================================================================
    // MÉTODOS DE VALIDAÇÃO ISOLADOS (Cada um gerencia sua transação na própria thread)
    // =========================================================================

    @Transactional
    protected void verificarExcessoVelocidade(Telemetria telemetria) {
        // Implemente sua lógica de salvar alerta no banco aqui se a velocidade estourar
    }

    @Transactional
    protected void verificarNivelCombustivel(Telemetria telemetria) {
        // Implemente sua lógica de nível crítico de combustível aqui
    }

    @Transactional
    protected void verificarGpsSemSinal(Telemetria telemetria) {
        // Implemente sua lógica de perda de sinal de GPS aqui
    }

    @Transactional
    protected void resolverAlertas(Telemetria telemetria) {
        // Implemente a lógica para limpar alertas que não são mais válidos
    }

    // =========================================================================
    // MÉTODOS JÁ EXISTENTES DE CONSULTA
    // =========================================================================

    public Optional<Telemetria> buscarUltimaPorVeiculo(Long veiculoId) {
        return telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId);
    }

    public List<Telemetria> listarPorVeiculo(Long veiculoId) {
        return telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId);
    }

    public List<Telemetria> listarPorPeriodo(Long veiculoId, LocalDateTime inicio, LocalDateTime fim) {
        return telemetriaRepository.findByVeiculoIdAndDataHoraBetween(veiculoId, inicio, fim);
    }

    // ✅ RF06 - Veículos sem sinal (30min + ignição)
    public List<Long> findVeiculosSemSinal(int minutosSemSinal, Boolean ultimaIgnicaoOn) {
        return telemetriaRepository.findVeiculosSemSinal(minutosSemSinal, ultimaIgnicaoOn);
    }
    
    @Transactional
    public void atualizarPosicaoAtual(Long veiculoId, Long tenantId, String veiculoUuid,
            Double latitude, Double longitude, Double velocidade, Double direcao,
            Boolean ignicao, LocalDateTime ultimaTelemetria) {
        System.out.println("📍 [RF06] UPSERT posição atual - Veículo: " + veiculoId);
        posicaoAtualRepository.upsertPosicaoAtual(veiculoId, tenantId, veiculoUuid,
                latitude, longitude, velocidade, direcao, ignicao, "ONLINE", ultimaTelemetria);
        System.out.println("✅ [RF06] Posição atualizada com sucesso");
    }
}