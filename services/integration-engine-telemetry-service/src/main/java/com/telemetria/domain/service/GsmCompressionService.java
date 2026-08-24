package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.enums.QualidadeSinalGsm;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;


@Service
public class GsmCompressionService {

    private static final Logger log = LoggerFactory.getLogger(GsmCompressionService.class);

    private final TelemetriaRepository telemetriaRepository;
    private final Map<String, Queue<Telemetria>> bufferMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> bufferTimestampMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> ultimoEnvioReduzido = new ConcurrentHashMap<>();

    @Value("${gsm.rssi.normal:-85}")
    private int rssiNormal;

    @Value("${gsm.rssi.reduced:-95}")
    private int rssiReduced;

    @Value("${gsm.buffer.max.size:1000}")
    private int bufferMaxSize;

    @Value("${gsm.buffer.ttl.minutes:1440}")
    private int bufferTtlMinutes;

    public GsmCompressionService(TelemetriaRepository telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
    }

    /**
     * Aplica política de compressão adaptativa (RN-TEL-004)
     * @return true = processa agora, false = armazenar em buffer
     */
    public boolean aplicarPoliticaGsm(Telemetria telemetria) {
        Double rssi = telemetria.getSinalGsm();
        String deviceId = telemetria.getDeviceId();

        if (deviceId == null) {
            return true;
        }

        // Sinal muito baixo ou ausente -> buffer
        if (rssi == null || rssi < rssiReduced) {
            log.info("Sinal baixo/ausente ({} dBm). Buffer para device {}", rssi, deviceId);
            adicionarAoBuffer(deviceId, telemetria);
            return false;
        }

        // Entre -85 e -95 dBm reduz a frequência para uma posição a cada 30 segundos.
        // Não há descarte: os pontos intermediários ficam no FIFO local até a reconexão.
        if (rssi < rssiNormal) {
            LocalDateTime ultimo = ultimoEnvioReduzido.get(deviceId);
            if (ultimo != null && Duration.between(ultimo, LocalDateTime.now()).getSeconds() < 30) {
                adicionarAoBuffer(deviceId, telemetria);
                return false;
            }
            ultimoEnvioReduzido.put(deviceId, LocalDateTime.now());
        } else {
            ultimoEnvioReduzido.remove(deviceId);
        }

        // Sinal normal/reduzido: processa agora e esvazia buffer se existir.
        if (bufferMap.containsKey(deviceId) && !bufferMap.get(deviceId).isEmpty()) {
            log.info("Reconexão detectada (RSSI {} dBm). Enviando buffer...", rssi);
            reenviarBuffer(deviceId);
        }

        return true;
    }

    private void adicionarAoBuffer(String deviceId, Telemetria telemetria) {
        Queue<Telemetria> buffer = bufferMap.computeIfAbsent(deviceId, k -> new LinkedList<>());
        
        if (buffer.size() >= bufferMaxSize) {
            Telemetria descartada = buffer.poll();
            log.warn("Buffer cheio para device {}. Descartando telemetria antiga", deviceId);
        }
        
        buffer.offer(telemetria);
        bufferTimestampMap.put(deviceId, LocalDateTime.now());
        log.debug("Buffer device {}: {} mensagens", deviceId, buffer.size());
    }

    @org.springframework.transaction.annotation.Transactional
    public void reenviarBuffer(String deviceId) {
        Queue<Telemetria> buffer = bufferMap.get(deviceId);
        if (buffer == null || buffer.isEmpty()) return;

        List<Telemetria> lote = new ArrayList<>();
        while (!buffer.isEmpty()) {
            Telemetria t = buffer.poll();
            t.setProcessadoEm(LocalDateTime.now());
            lote.add(t);
        }

        try {
            telemetriaRepository.saveAll(lote);
            bufferMap.remove(deviceId);
            bufferTimestampMap.remove(deviceId);
            log.info("Buffer reenviado: device={}, msgs={}", deviceId, lote.size());
        } catch (Exception e) {
            // Rollback: recoloca mensagens no buffer
            buffer.addAll(lote);
            log.error("Falha ao reenviar buffer", e);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void retryBuffers() {
        // O buffer só é reenviado quando chega um pacote com sinal recuperado. Assim,
        // não quebramos o contrato de FIFO nem enviamos dados enquanto o modem permanece off-line.
    }

    @Scheduled(fixedDelay = 3600000)
    public void limparBuffersExpirados() {
        LocalDateTime agora = LocalDateTime.now();
        List<String> expirados = bufferTimestampMap.entrySet().stream()
            .filter(entry -> Duration.between(entry.getValue(), agora).toMinutes() > bufferTtlMinutes)
            .map(Map.Entry::getKey)
            .toList();
        
        expirados.forEach(deviceId -> {
            bufferMap.remove(deviceId);
            bufferTimestampMap.remove(deviceId);
            log.warn("Buffer expirado removido: {}", deviceId);
        });
    }

    private QualidadeSinalGsm classificarQualidadeSinal(Double rssi) {
        if (rssi == null || rssi < rssiReduced) return QualidadeSinalGsm.SEM_SINAL;
        if (rssi < rssiNormal) return QualidadeSinalGsm.REDUZIDO;
        return QualidadeSinalGsm.NORMAL;
    }
}
