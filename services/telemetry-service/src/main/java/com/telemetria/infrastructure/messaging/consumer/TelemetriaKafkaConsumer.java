package com.telemetria.infrastructure.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.telemetria.application.service.BackpressureMonitorService;
import com.telemetria.application.service.TelemetriaProcessingResult;
import com.telemetria.application.service.TelemetriaProcessor;
import com.telemetria.infrastructure.messaging.dto.KafkaMessageMetadata;
import com.telemetria.infrastructure.metrics.TelemetriaMetrics;

import io.micrometer.core.instrument.Timer;

/**
 * Listener de ingestão. A responsabilidade termina depois que telemetria e
 * outbox são confirmados na mesma transação; enriquecimentos usam outro tópico.
 */
@Service
public class TelemetriaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaKafkaConsumer.class);

    private final TelemetriaProcessor processor;
    private final BackpressureMonitorService backpressureMonitor;
    private final TelemetriaMetrics metrics;

    public TelemetriaKafkaConsumer(
            TelemetriaProcessor processor,
            BackpressureMonitorService backpressureMonitor,
            TelemetriaMetrics metrics) {
        this.processor = processor;
        this.backpressureMonitor = backpressureMonitor;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.telemetria-raw:telemetria-raw}",
            groupId = "${spring.kafka.consumer.group-id:telemetria-group}",
            concurrency = "${telemetria.kafka.concurrency:8}")
    public void consume(ConsumerRecord<String, String> record) {
        Timer.Sample timer = metrics.iniciarTimer();
        long startedAt = System.currentTimeMillis();
        metrics.incrementarRecebidas();
        backpressureMonitor.registrarRecebimento();

        try {
            applyBackpressure();
            KafkaMessageMetadata metadata = new KafkaMessageMetadata(
                    record.topic(), record.partition(), record.offset());
            TelemetriaProcessingResult result = processor.process(record.value(), metadata);

            MDC.put("eventId", result.eventId());
            MDC.put("veiculoId", String.valueOf(result.veiculoId()));

            if (result.duplicate()) {
                metrics.incrementarDuplicadas();
                log.info("Telemetria duplicada ignorada: status={}, eventId={}, veículo={}",
                        result.status(), result.eventId(), result.veiculoId());
                return;
            }

            if (result.outOfOrder()) {
                metrics.incrementarForaDeOrdem();
            }
            metrics.registrarSequenciasAusentes(result.sequenceGap());
            metrics.incrementarProcessadas();
            long elapsed = System.currentTimeMillis() - startedAt;
            backpressureMonitor.registrarProcessamento(elapsed);
            log.info("Telemetria persistida: id={}, eventId={}, veículo={}, foraDeOrdem={}, gap={}, tempo={}ms",
                    result.telemetriaId(), result.eventId(), result.veiculoId(), result.outOfOrder(),
                    result.sequenceGap(), elapsed);
        } finally {
            metrics.pararTimer(timer);
            MDC.remove("eventId");
            MDC.remove("veiculoId");
        }
    }

    private void applyBackpressure() {
        try {
            backpressureMonitor.aplicarBackpressure();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ingestão interrompida durante o backpressure", e);
        }
    }
}
