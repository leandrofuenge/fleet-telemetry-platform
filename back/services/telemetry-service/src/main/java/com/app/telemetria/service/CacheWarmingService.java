package com.app.telemetria.service;

import com.app.telemetria.config.CacheConfig;
import com.app.telemetria.entity.Motorista;
import com.app.telemetria.entity.Rota;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.repository.MotoristaRepository;
import com.app.telemetria.repository.RotaRepository;
import com.app.telemetria.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CacheWarmingService {

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
    }

    /**
     * Método principal de warming - carrega dados em todos os caches
     */
    public void warmUpAllCaches() {
        System.out.println("🚀 Iniciando Cache Warming...");
        long inicio = System.currentTimeMillis();

        // Executa em paralelo para ser mais rápido
        CompletableFuture<Void> veiculosFuture = CompletableFuture.runAsync(this::warmUpVeiculos);
        CompletableFuture<Void> rotasFuture = CompletableFuture.runAsync(this::warmUpRotas);
        CompletableFuture<Void> motoristasFuture = CompletableFuture.runAsync(this::warmUpMotoristas);

        // Aguarda todos completarem
        CompletableFuture.allOf(veiculosFuture, rotasFuture, motoristasFuture).join();

        long fim = System.currentTimeMillis();
        System.out.println("✅ Cache Warming concluído em " + (fim - inicio) + " ms");
        logCacheStats();
    }

    /**
     * Aquecimento de veículos
     */
    private void warmUpVeiculos() {
        String cacheName = CacheConfig.CACHE_VEICULOS;
        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            System.err.println("❌ Cache " + cacheName + " não encontrado");
            return;
        }

        List<Veiculo> veiculos = veiculoRepository.findAll();
        AtomicInteger contador = new AtomicInteger(0);

        veiculos.parallelStream().forEach(veiculo -> {
            // Cache no Caffeine (local)
            caffeineCache.put(veiculo.getId(), veiculo);
            // Cache no Redis (distribuído)
            redisCache.put(veiculo.getId(), veiculo);

            if (contador.incrementAndGet() % 10 == 0) {
                System.out.println("🔄 " + contador.get() + "/" + veiculos.size() + " veículos carregados");
            }
        });

        System.out.println("✅ " + veiculos.size() + " veículos carregados em cache");
    }

    /**
     * Aquecimento de rotas
     */
    private void warmUpRotas() {
        String cacheName = CacheConfig.CACHE_ROTAS;
        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            System.err.println("❌ Cache " + cacheName + " não encontrado");
            return;
        }

        List<Rota> rotas = rotaRepository.findAll();
        rotas.parallelStream().forEach(rota -> {
            caffeineCache.put(rota.getId(), rota);
            redisCache.put(rota.getId(), rota);
        });

        System.out.println("✅ " + rotas.size() + " rotas carregadas em cache");
    }

    /**
     * Aquecimento de motoristas
     */
    private void warmUpMotoristas() {
        String cacheName = CacheConfig.CACHE_MOTORISTAS;
        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        Cache redisCache = redisCacheManager.getCache(cacheName);

        if (caffeineCache == null || redisCache == null) {
            System.err.println("❌ Cache " + cacheName + " não encontrado");
            return;
        }

        List<Motorista> motoristas = motoristaRepository.findAll();
        motoristas.parallelStream().forEach(motorista -> {
            caffeineCache.put(motorista.getId(), motorista);
            redisCache.put(motorista.getId(), motorista);
        });

        System.out.println("✅ " + motoristas.size() + " motoristas carregados em cache");
    }

    /**
     * Log de estatísticas dos caches
     */
    private void logCacheStats() {
        System.out.println("\n📊 ESTATÍSTICAS DE CACHE");
        System.out.println("========================");

        try {
            // Caffeine stats
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>) caffeineCacheManager
                    .getCache(CacheConfig.CACHE_VEICULOS).getNativeCache();

            var stats = nativeCache.stats();
            System.out.println("Caffeine Stats:");
            System.out.println("  - Hit rate: " + String.format("%.2f", stats.hitRate() * 100) + "%");
            System.out.println("  - Hits: " + stats.hitCount());
            System.out.println("  - Misses: " + stats.missCount());
            System.out.println("  - Tamanho estimado: " + nativeCache.estimatedSize());
        } catch (Exception e) {
            System.err.println("Erro ao obter estatísticas: " + e.getMessage());
        }
    }
}