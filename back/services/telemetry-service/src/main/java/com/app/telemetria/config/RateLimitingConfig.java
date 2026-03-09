package com.app.telemetria.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {
    
    // Cache de buckets por veículo ou IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Limites configuráveis
    private static final int LIMITE_PADRAO = 10;   // 10 requisições
    private static final int PERIODO_PADRAO = 60;  // por minuto
    private static final int LIMITE_BURST = 2;     // 2 requisições
    private static final int PERIODO_BURST = 1;    // por segundo
    
    public Bucket resolveBucket(String chave) {
        return buckets.computeIfAbsent(chave, this::criarBucket);
    }
    
    private Bucket criarBucket(String chave) {
        System.out.println("🪣 Criando bucket rate limiting para: " + chave);
        
        Bandwidth limitePadrao = Bandwidth.classic(
            LIMITE_PADRAO, 
            Refill.greedy(LIMITE_PADRAO, Duration.ofSeconds(PERIODO_PADRAO))
        );
        
        Bandwidth limiteBurst = Bandwidth.classic(
            LIMITE_BURST,
            Refill.greedy(LIMITE_BURST, Duration.ofSeconds(PERIODO_BURST))
        );
        
        return Bucket.builder()   // ✅ CORRETO NA VERSÃO 8.x
                .addLimit(limitePadrao)
                .addLimit(limiteBurst)
                .build();
    }
    
    public boolean tryConsume(String chave) {
        Bucket bucket = resolveBucket(chave);
        return bucket.tryConsume(1);
    }
    
    public long getAvailableTokens(String chave) {
        Bucket bucket = resolveBucket(chave);
        return bucket.getAvailableTokens();
    }
}