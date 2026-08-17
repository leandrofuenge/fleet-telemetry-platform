package com.telemetria.infrastructure.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.application.service.TelemetriaEventProcessor;
import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.infrastructure.messaging.dto.TelemetriaPersistidaEvent;

@Service
public class TelemetriaEventConsumer {

    private final TelemetriaEventProcessor processor;
    private final ObjectMapper objectMapper;

    public TelemetriaEventConsumer(TelemetriaEventProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.telemetria-events:telemetria-events}",
            groupId = "${telemetria.events.consumer.group-id:telemetria-enrichment-group}",
            concurrency = "${telemetria.events.consumer.concurrency:4}")
    public void consume(ConsumerRecord<String, String> record) {
        TelemetriaPersistidaEvent event = deserialize(record.value());
        try {
            MDC.put("eventId", event.eventId());
            MDC.put("outboxEventId", event.outboxEventId());
            MDC.put("veiculoId", String.valueOf(event.veiculoId()));
            processor.process(event);
        } finally {
            MDC.remove("eventId");
            MDC.remove("outboxEventId");
            MDC.remove("veiculoId");
        }
    }

    private TelemetriaPersistidaEvent deserialize(String payload) {
        try {
            TelemetriaPersistidaEvent event = objectMapper.readValue(payload, TelemetriaPersistidaEvent.class);
            if (event.outboxEventId() == null || event.telemetriaId() == null || event.eventId() == null) {
                throw new TelemetriaMessageException("Evento de telemetria incompleto");
            }
            return event;
        } catch (JsonProcessingException e) {
            throw new TelemetriaMessageException("Evento de telemetria inválido", e);
        }
    }
}
