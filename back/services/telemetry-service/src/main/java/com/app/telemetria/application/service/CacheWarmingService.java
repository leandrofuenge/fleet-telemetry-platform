package com.app.telemetria.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.api.dto.response.VeiculoCacheDTO;
import com.app.telemetria.domain.entity.Motorista;
import com.app.telemetria.domain.entity.Rota;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.infrastructure.cache.CacheConfig;
import com.app.telemetria.infrastructure.persistence.MotoristaRepository;
import com.app.telemetria.infrastructure.persistence.RotaRepository;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;

@Service
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final VeiculoRepository veiculoRepository;
    private final RotaRepository rotaRepository;
    private final MotoristaRepository motoristaRepository;

    public CacheWarmingService(
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            VeiculoRepository veiculoRepository,
            RotaRepository rotaRepository,
            MotoristaRepository motoristaRepository) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.veiculoRepository = veiculoRepository;
        this.rotaRepository = rotaRepository;
        this.motoristaRepository = motoristaRepository;
        
        log.info("✅ CacheWarmingService inicializado");
        log.debug("📊 Cache managers - Caffeine: {}, Redis: {}", 
                 caffeineCacheManager != null ? "presente" : "ausente",
                 redisCacheManager != null ? "presente" : "ausente");
    }

    /**
     * Método principal de warming - carrega dados em todos os caches
     */
    @Transactional(readOnly = true)
    public void warmUpAllCaches() {
        log.info("🚀 Iniciando Cache Warming...");
        long inicio = System.currentTimeMillis();

        log.debug("📝 Verificando caches disponíveis...");
        verificarCachesDisponiveis();

        log.debug("🔄 Executando warming em paralelo para veículos, rotas e motoristas");

        CompletableFuture<Void> veiculosFuture = CompletableFuture.runAsync(() -> {
            log.debug("🔄 Thread de warming de veículos iniciada");
            warmUpVeiculos();
        });

        CompletableFuture<Void> rotasFuture = CompletableFuture.runAsync(() -> {
            log.debug("🔄 Thread de warming de rotas iniciada");
            warmUpRotas();
        });

        CompletableFuture<Void> motoristasFuture = CompletableFuture.runAsync(() -> {
            log.debug("🔄 Thread de warming de motoristas iniciada");
            warmUpMotoristas();
        });

        log.debug("⏳ Aguardando conclusão de todas as threads de warming...");
        CompletableFuture.allOf(veiculosFuture, rotasFuture, motoristasFuture).join();

        long fim = System.currentTimeMillis();
        log.info("✅ Cache Warming concluído em {} ms", (fim - inicio));
        logCacheStats();
    }

    private void verificarCachesDisponiveis() {
        String[] caches = {
            CacheConfig.CACHE_VEICULOS,
            CacheConfig.CACHE_ROTAS,
            CacheConfig.CACHE_MOTORISTAS
        };

        for (String cacheName : caches) {
            Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
            Cache redisCache = redisCacheManager.getCache(cacheName);

            if (caffeineCache == null) {
                log.error("❌ Cache Caffeine '{}' não encontrado", cacheName);
            } else {
                log.debug("✅ Cache Caffeine '{}' disponível", cacheName);
            }

            if (redisCache == null) {
                log.error("❌ Cache Redis '{}' não encontrado", cacheName);
            } else {
                log.debug("✅ Cache Redis '{}' disponível", cacheName);
            }
        }
    }

    @Transactional(readOnly = true)
    private void warmUpVeiculos() {
        String cacheName = CacheConfig.CACHE_VEICULOS;
        log.debug("🚛 Iniciando warming de veículos para cache '{}'", cacheName);
        
        long inicio = System.currentTimeMillis();

        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            log.error("❌ Cache '{}' não encontrado - Caffeine: {}, Redis: {}", 
                     cacheName, caffeineCache != null, redisCache != null);
            return;
        }

        log.debug("📊 Buscando veículos do banco de dados (com cliente e motorista atual)...");
        List<Veiculo> veiculos = veiculoRepository.findAllWithClienteAndMotorista();
        log.debug("📊 Total de veículos encontrados: {}", veiculos.size());

        if (veiculos.isEmpty()) {
            log.warn("⚠️ Nenhum veículo encontrado para cache");
            return;
        }

        List<VeiculoCacheDTO> veiculosDTO = veiculos.stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());

        AtomicInteger contador = new AtomicInteger(0);
        int total = veiculosDTO.size();

        veiculosDTO.parallelStream().forEach(veiculoDTO -> {
            caffeineCache.put(veiculoDTO.getId(), veiculoDTO);
            redisCache.put(veiculoDTO.getId(), veiculoDTO);

            int atual = contador.incrementAndGet();
            if (atual % 10 == 0 || atual == total) {
                double percentual = (atual * 100.0) / total;
                log.debug("🔄 Veículos carregados: {}/{} ({:.1f}%)", atual, total, percentual);
            }
            
            log.trace("✅ Veículo {} carregado em cache", veiculoDTO.getId());
        });

        long fim = System.currentTimeMillis();
        log.info("✅ {} veículos carregados em cache em {}ms", total, (fim - inicio));
    }

    private VeiculoCacheDTO converterParaDTO(Veiculo veiculo) {
        VeiculoCacheDTO dto = new VeiculoCacheDTO();
        
        dto.setId(veiculo.getId());
        dto.setPlaca(veiculo.getPlaca());
        dto.setMarca(veiculo.getMarca());
        dto.setModelo(veiculo.getModelo());
        dto.setAnoFabricacao(veiculo.getAnoFabricacao());
        dto.setAtivo(veiculo.getAtivo());
        
        if (veiculo.getCapacidadeCarga() != null) {
            dto.setCapacidadeCarga(veiculo.getCapacidadeCarga().doubleValue());
        }
        
        dto.setCriadoEm(veiculo.getCriadoEm());
        dto.setAtualizadoEm(veiculo.getAtualizadoEm());
        
        if (veiculo.getCliente() != null) {
            dto.setClienteId(veiculo.getCliente().getId());
            dto.setClienteNome(veiculo.getCliente().getNomeRazaoSocial());
        }
        
        if (veiculo.getMotoristaAtual() != null) {
            dto.setMotoristaId(veiculo.getMotoristaAtual().getId());
            dto.setMotoristaNome(veiculo.getMotoristaAtual().getNome());
            dto.setMotoristaCpf(veiculo.getMotoristaAtual().getCpf());
            dto.setMotoristaCnh(veiculo.getMotoristaAtual().getCnh());
        }
        
        return dto;
    }

    @Transactional(readOnly = true)
    private void warmUpRotas() {
        String cacheName = CacheConfig.CACHE_ROTAS;
        log.debug("🛣️ Iniciando warming de rotas para cache '{}'", cacheName);
        
        long inicio = System.currentTimeMillis();

        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            log.error("❌ Cache '{}' não encontrado", cacheName);
            return;
        }

        log.debug("📊 Buscando rotas do banco de dados...");
        List<Rota> rotas = rotaRepository.findAll();
        log.debug("📊 Total de rotas encontradas: {}", rotas.size());

        if (rotas.isEmpty()) {
            log.warn("⚠️ Nenhuma rota encontrada para cache");
            return;
        }

        AtomicInteger contador = new AtomicInteger(0);
        int total = rotas.size();

        rotas.parallelStream().forEach(rota -> {
            caffeineCache.put(rota.getId(), rota);
            redisCache.put(rota.getId(), rota);

            int atual = contador.incrementAndGet();
            if (atual % 20 == 0 || atual == total) {
                log.debug("🔄 Rotas carregadas: {}/{}", atual, total);
            }
            
            log.trace("✅ Rota {} carregada em cache", rota.getId());
        });

        long fim = System.currentTimeMillis();
        log.info("✅ {} rotas carregadas em cache em {}ms", total, (fim - inicio));
    }

    @Transactional(readOnly = true)
    private void warmUpMotoristas() {
        String cacheName = CacheConfig.CACHE_MOTORISTAS;
        log.debug("👤 Iniciando warming de motoristas para cache '{}'", cacheName);
        
        long inicio = System.currentTimeMillis();

        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            log.error("❌ Cache '{}' não encontrado", cacheName);
            return;
        }

        log.debug("📊 Buscando motoristas do banco de dados...");
        List<Motorista> motoristas = motoristaRepository.findAll();
        log.debug("📊 Total de motoristas encontrados: {}", motoristas.size());

        if (motoristas.isEmpty()) {
            log.warn("⚠️ Nenhum motorista encontrado para cache");
            return;
        }

        AtomicInteger contador = new AtomicInteger(0);
        int total = motoristas.size();

        motoristas.parallelStream().forEach(motorista -> {
            caffeineCache.put(motorista.getId(), motorista);
            redisCache.put(motorista.getId(), motorista);

            int atual = contador.incrementAndGet();
            if (atual % 20 == 0 || atual == total) {
                log.debug("🔄 Motoristas carregados: {}/{}", atual, total);
            }
            
            log.trace("✅ Motorista {} carregado em cache", motorista.getId());
        });

        long fim = System.currentTimeMillis();
        log.info("✅ {} motoristas carregados em cache em {}ms", total, (fim - inicio));
    }

    private void logCacheStats() {
        log.info("\n📊 ESTATÍSTICAS DE CACHE");
        log.info("========================");

        try {
            logCacheInfo(CacheConfig.CACHE_VEICULOS, "Veículos");
            logCacheInfo(CacheConfig.CACHE_ROTAS, "Rotas");
            logCacheInfo(CacheConfig.CACHE_MOTORISTAS, "Motoristas");
            logCaffeineStats();

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas de cache: {}", e.getMessage(), e);
        }
    }

    private void logCacheInfo(String cacheName, String descricao) {
        try {
            Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
            Cache redisCache = redisCacheManager.getCache(cacheName);

            log.debug("📦 Cache '{}' ({})", cacheName, descricao);
            log.debug("   - Caffeine: {}", caffeineCache != null ? "disponível" : "indisponível");
            log.debug("   - Redis: {}", redisCache != null ? "disponível" : "indisponível");

        } catch (Exception e) {
            log.warn("⚠️ Não foi possível obter info do cache {}: {}", cacheName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void logCaffeineStats() {
        try {
            Object nativeCacheObj = caffeineCacheManager.getCache(CacheConfig.CACHE_VEICULOS).getNativeCache();
            if (nativeCacheObj instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCacheObj;
                
                var stats = nativeCache.stats();
                
                log.info("📊 Estatísticas Caffeine:");
                log.info("   - Hit rate: {:.2f}%", stats.hitRate() * 100);
                log.info("   - Hits: {}", stats.hitCount());
                log.info("   - Misses: {}", stats.missCount());
                log.info("   - Total de requisições: {}", stats.requestCount());
                log.info("   - Tamanho estimado: {}", nativeCache.estimatedSize());
                log.info("   - Taxa de evicção: {:.2f}", stats.evictionCount());
                log.info("   - Tempo médio de carga: {:.2f} ns", stats.averageLoadPenalty());
            }
        } catch (Exception e) {
            log.warn("⚠️ Não foi possível obter estatísticas detalhadas do Caffeine: {}", e.getMessage());
        }
    }

    public void warmUpCache(String cacheName) {
        log.info("🔥 Iniciando warming seletivo para cache '{}'", cacheName);
        
        switch (cacheName) {
            case CacheConfig.CACHE_VEICULOS:
                warmUpVeiculos();
                break;
            case CacheConfig.CACHE_ROTAS:
                warmUpRotas();
                break;
            case CacheConfig.CACHE_MOTORISTAS:
                warmUpMotoristas();
                break;
            default:
                log.warn("⚠️ Cache '{}' não reconhecido para warming seletivo", cacheName);
        }
    }

    public void clearAllCaches() {
        log.info("🧹 Iniciando limpeza de todos os caches");
        
        String[] caches = {
            CacheConfig.CACHE_VEICULOS,
            CacheConfig.CACHE_ROTAS,
            CacheConfig.CACHE_MOTORISTAS
        };

        for (String cacheName : caches) {
            try {
                Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
                Cache redisCache = redisCacheManager.getCache(cacheName);

                if (caffeineCache != null) {
                    caffeineCache.clear();
                    log.debug("✅ Cache Caffeine '{}' limpo", cacheName);
                }

                if (redisCache != null) {
                    redisCache.clear();
                    log.debug("✅ Cache Redis '{}' limpo", cacheName);
                }

            } catch (Exception e) {
                log.error("❌ Erro ao limpar cache '{}': {}", cacheName, e.getMessage());
            }
        }

        log.info("✅ Limpeza de caches concluída");
    }
}