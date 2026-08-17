package com.telemetria.infrastructure.messaging.producer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.TelemetriaOutboxEvent;
import com.telemetria.infrastructure.persistence.TelemetriaOutboxRepository;

@Component
public class TelemetriaOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaOutboxPublisher.class);

    private final TelemetriaOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;
    private final int timeoutSeconds;
    private final int maxAttempts;
    private final long initialRetryMillis;

    public TelemetriaOutboxPublisher(
            TelemetriaOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${spring.kafka.topic.telemetria-events:telemetria-events}") String topic,
            @Value("${telemetria.outbox.batch-size:50}") int batchSize,
            @Value("${telemetria.outbox.publish-timeout-seconds:10}") int timeoutSeconds,
            @Value("${telemetria.outbox.max-attempts:10}") int maxAttempts,
            @Value("${telemetria.outbox.retry-initial-interval-ms:1000}") long initialRetryMillis) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
        this.timeoutSeconds = timeoutSeconds;
        this.maxAttempts = maxAttempts;
        this.initialRetryMillis = initialRetryMillis;
    }

    @Scheduled(fixedDelayString = "${telemetria.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        List<TelemetriaOutboxEvent> events = repository.findPending(
                LocalDateTime.now(), PageRequest.of(0, batchSize));

        for (TelemetriaOutboxEvent event : events) {
            try {
                kafkaTemplate.send(topic, event.getVeiculoId().toString(), event.getPayload())
                        .get(timeoutSeconds, TimeUnit.SECONDS);
                event.markPublished();
                log.debug("Evento outbox publicado: outboxId={}, eventId={}, veículo={}",
                        event.getId(), event.getEventId(), event.getVeiculoId());
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.scheduleRetry(e.getMessage(), maxAttempts, initialRetryMillis);
                log.warn("Falha ao publicar outbox {} (tentativa {}): {}",
                        event.getId(), event.getTentativas(), e.getMessage());
            }
        }
        repository.saveAll(events);
    }
}
