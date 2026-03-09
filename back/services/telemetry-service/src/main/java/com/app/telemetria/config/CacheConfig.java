package com.app.telemetria.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_VEICULOS = "veiculos";
    public static final String CACHE_ROTAS = "rotas";
    public static final String CACHE_MOTORISTAS = "motoristas";
    public static final String CACHE_GEOCODING = "geocoding";
    public static final String CACHE_ALERTAS = "alertas";

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        System.out.println("⚙️ Configurando Caffeine Cache Manager...");
        
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            CACHE_VEICULOS, CACHE_ROTAS, CACHE_MOTORISTAS, CACHE_GEOCODING, CACHE_ALERTAS
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(500)
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .softValues());
        
        cacheManager.setAllowNullValues(false);
        
        System.out.println("✅ Caffeine Cache Manager configurado");
        return cacheManager;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        System.out.println("⚙️ Configurando Redis Cache Manager...");
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .prefixCacheNameWith("telemetria:");

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration(CACHE_VEICULOS, 
                    config.entryTtl(Duration.ofMinutes(120)))
                .withCacheConfiguration(CACHE_ROTAS, 
                    config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration(CACHE_MOTORISTAS, 
                    config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration(CACHE_GEOCODING, 
                    config.entryTtl(Duration.ofHours(24)))
                .build();
        
        System.out.println("✅ Redis Cache Manager configurado");
        return cacheManager;
    }
}