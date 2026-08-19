package com.telemetria.integration.senatran.serpro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Cache em memória com chave irreversível para evitar placa/RENAVAM em dumps e métricas. */
@Component
public class SerproConsultaCache {
    private final SerproProperties properties;
    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Autowired
    public SerproConsultaCache(SerproProperties properties) { this(properties, Clock.systemUTC()); }
    SerproConsultaCache(SerproProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public SerproVeiculoResponse get(String plate, String renavam) {
        String key = key(plate, renavam);
        Entry entry = entries.get(key);
        if (entry == null) return null;
        if (clock.millis() >= entry.expiresAt()) {
            entries.remove(key, entry);
            return null;
        }
        return entry.response();
    }

    public void put(String plate, String renavam, SerproVeiculoResponse response) {
        if (properties.getSerpro().getCacheTtl().isZero()
                || properties.getSerpro().getCacheTtl().isNegative()) return;
        if (entries.size() >= properties.getSerpro().getCacheMaxEntries()) removeExpiredOrOldest();
        entries.put(key(plate, renavam), new Entry(response,
                clock.millis() + properties.getSerpro().getCacheTtl().toMillis(), clock.millis()));
    }

    private void removeExpiredOrOldest() {
        long now = clock.millis();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (entries.size() < properties.getSerpro().getCacheMaxEntries()) return;
        entries.entrySet().stream().min(Map.Entry.comparingByValue(
                java.util.Comparator.comparingLong(Entry::createdAt))).ifPresent(entry -> entries.remove(entry.getKey()));
    }

    private String key(String plate, String renavam) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((plate + "|" + renavam).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private record Entry(SerproVeiculoResponse response, long expiresAt, long createdAt) {}
}
