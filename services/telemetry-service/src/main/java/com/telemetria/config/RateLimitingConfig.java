package com.telemetria.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Configuration
public class RateLimitingConfig {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    private static final int LIMITE_PADRAO = 10;
    private static final int PERIODO_PADRAO = 60;
    private static final int LIMITE_BURST = 2;
    private static final int PERIODO_BURST = 1;
    
    public Bucket resolveBucket(String chave) {
        return buckets.computeIfAbsent(chave, this::criarBucket);
    }
    
    private Bucket criarBucket(String chave) {
        System.out.println("🪣 Criando bucket rate limiting para: " + chave);
        
        // Usando os novos métodos não-deprecated
        Bandwidth limitePadrao = Bandwidth.builder()
                .capacity(LIMITE_PADRAO)
                .refillIntervally(LIMITE_PADRAO, Duration.ofSeconds(PERIODO_PADRAO))
                .build();
        
        Bandwidth limiteBurst = Bandwidth.builder()
                .capacity(LIMITE_BURST)
                .refillIntervally(LIMITE_BURST, Duration.ofSeconds(PERIODO_BURST))
                .build();
        
        return Bucket.builder()
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