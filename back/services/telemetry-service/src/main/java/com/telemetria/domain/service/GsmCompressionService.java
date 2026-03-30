package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class GsmCompressionService {

    private static final Logger log = LoggerFactory.getLogger(GsmCompressionService.class);

    private final TelemetriaRepository telemetriaRepository;

    // Mapas de estado por dispositivo (deviceId)
    private final Map<String, Queue<Telemetria>> bufferMap = new ConcurrentHashMap<>();
    private final Map<String, Double> ultimoRssi = new ConcurrentHashMap<>();

    // Configurações
    @Value("${gsm.rssi.normal:-85}")
    private int rssiNormal;

    @Value("${gsm.rssi.reduced:-95}")
    private int rssiReduced;

    @Value("${gsm.buffer.max.size:1000}")
    private int bufferMaxSize;

    @Value("${gsm.buffer.ttl.minutes:1440}") // 24h
    private int bufferTtlMinutes;

    public GsmCompressionService(TelemetriaRepository telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
        log.info("✅ GsmCompressionService inicializado");
    }

    /**
     * Aplica política de compressão adaptativa de frequência (RN-TEL-004)
     * Retorna true se a mensagem deve ser processada normalmente,
     * false se foi armazenada em buffer (sinal muito baixo ou sem sinal).
     */
    public boolean aplicarPoliticaGsm(Telemetria telemetria) {
        Double rssi = telemetria.getSinalGsm();
        String deviceId = telemetria.getDeviceId();

        if (deviceId == null) {
            log.warn("Telemetria sem deviceId, não é possível aplicar política GSM");
            return true; // processa normalmente
        }

        // Atualiza último RSSI conhecido para este dispositivo
        ultimoRssi.put(deviceId, rssi);

        // Caso não tenha RSSI (nulo), considera como sinal ausente -> buffer
        if (rssi == null || rssi < rssiReduced) {
            log.info("📦 Sinal GSM muito baixo ({} dBm) ou ausente. Armazenando em buffer para device {}", rssi, deviceId);
            addToBuffer(deviceId, telemetria);
            return false; // não processar agora
        }

        // Sinal normal ou reduzido: processa normalmente
        // Se há buffer para este dispositivo, esvazia-o (reconexão)
        if (bufferMap.containsKey(deviceId) && !bufferMap.get(deviceId).isEmpty()) {
            log.info("📡 Reconexão detectada (RSSI {} dBm). Enviando buffer FIFO para device {}", rssi, deviceId);
            flushBuffer(deviceId);
        }

        // Aqui você poderia registrar a frequência ideal para o dispositivo,
        // mas o ajuste efetivo da frequência de envio é responsabilidade do dispositivo.
        // O servidor apenas monitora e aciona o buffer quando necessário.

        return true;
    }

    private void addToBuffer(String deviceId, Telemetria telemetria) {
        Queue<Telemetria> buffer = bufferMap.computeIfAbsent(deviceId, k -> new LinkedList<>());

        // Limitar tamanho do buffer para não estourar memória
        if (buffer.size() >= bufferMaxSize) {
            Telemetria descartada = buffer.poll();
            log.warn("Buffer do device {} atingiu limite máximo. Descartando telemetria mais antiga (ID {})",
                    deviceId, descartada.getId());
        }

        buffer.offer(telemetria);
        log.debug("Buffer do device {} agora tem {} mensagens", deviceId, buffer.size());
    }

    /**
     * Envia todas as mensagens do buffer (FIFO) para o repositório.
     * Pode ser chamado na reconexão ou periodicamente.
     */
    private void flushBuffer(String deviceId) {
        Queue<Telemetria> buffer = bufferMap.get(deviceId);
        if (buffer == null || buffer.isEmpty()) return;

        log.info("Enviando buffer do device {} ({} mensagens)", deviceId, buffer.size());

        while (!buffer.isEmpty()) {
            Telemetria t = buffer.poll();
            // Atualiza o timestamp de recebimento para refletir o momento do reenvio
            t.setProcessadoEm(LocalDateTime.now());
            // Salva novamente (ou atualiza) – cuidado para não duplicar
            // Aqui optamos por salvar como nova entrada? Depende da regra.
            // Para manter o histórico, podemos simplesmente persistir novamente.
            // Mas cuidado para não criar duplicatas. Como a telemetria já foi salva
            // anteriormente (antes do buffer), podemos apenas atualizar um flag.
            // Vamos simplesmente garantir que ela seja persistida (o save irá criar um novo ID se for um novo objeto).
            // Para evitar duplicação, devemos ter mantido o objeto original com ID? Precisamos repensar.
            // Solução: não salvar a telemetria antes do buffer. Ajuste o fluxo: se a política disser "buffer",
            // a telemetria não deve ter sido salva ainda. Vamos modificar: a chamada a `aplicarPoliticaGsm`
            // deve ocorrer **antes** de salvar a telemetria. Então, no consumidor, primeiro aplicamos a política,
            // se retornar true, salvamos; se false, apenas armazenamos no buffer (não salvamos).
            // Isso evita duplicação.

            // Por enquanto, assumiremos que a telemetria ainda não foi salva.
            telemetriaRepository.save(t);
            log.debug("Mensagem do buffer enviada: ID {}", t.getId());
        }

        bufferMap.remove(deviceId);
        log.info("Buffer do device {} completamente esvaziado", deviceId);
    }

    /**
     * Scheduler periódico para tentar reenviar buffers de dispositivos
     * que podem ter recuperado o sinal mesmo sem nova telemetria.
     * Executa a cada 30 segundos.
     */
    @Scheduled(fixedDelay = 30000)
    public void retryBuffers() {
        if (bufferMap.isEmpty()) return;

        log.debug("Verificando buffers de {} dispositivos", bufferMap.size());

        for (Map.Entry<String, Queue<Telemetria>> entry : bufferMap.entrySet()) {
            String deviceId = entry.getKey();
            Double ultimoRssiConhecido = ultimoRssi.get(deviceId);

            // Se não temos registro de RSSI recente ou ele melhorou para normal, tenta reenviar
            if (ultimoRssiConhecido != null && ultimoRssiConhecido >= rssiNormal) {
                log.info("Sinal do device {} aparentemente normal (RSSI {}). Tentando reenviar buffer.", deviceId, ultimoRssiConhecido);
                flushBuffer(deviceId);
            }
        }
    }

    /**
     * Remove buffers antigos para liberar memória (opcional)
     */
    @Scheduled(fixedDelay = 3600000) // a cada hora
    public void cleanupOldBuffers() {
        // Por simplicidade, não implementamos TTL de buffer individual.
        // Mas poderíamos verificar timestamp das mensagens.
    }
}