package com.telemetria.integration.telemetry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Consome a outbox publicada pelo telemetry-service em um grupo independente. */
@Component
public class ConsumidorKafkaEventoTelemetria {

    private final ObjectMapper objectMapper;
    private final OrquestradorEventoTelemetria orquestrador;

    public ConsumidorKafkaEventoTelemetria(
            ObjectMapper objectMapper,
            OrquestradorEventoTelemetria orquestrador) {
        this.objectMapper = objectMapper;
        this.orquestrador = orquestrador;
    }

    @KafkaListener(
            topics = "${integration.telemetry-events.topic:telemetria-events}",
            groupId = "${integration.telemetry-events.consumer-group:integration-engine-telemetry-v1}",
            concurrency = "${integration.telemetry-events.concurrency:3}",
            autoStartup = "${integration.telemetry-events.enabled:false}")
    public void consumir(ConsumerRecord<String, String> record) throws Exception {
        EventoTelemetriaPersistida event = objectMapper.readValue(record.value(), EventoTelemetriaPersistida.class);
        orquestrador.processar(event);
    }
}
