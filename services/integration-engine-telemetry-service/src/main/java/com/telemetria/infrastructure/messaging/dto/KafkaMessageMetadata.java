package com.telemetria.infrastructure.messaging.dto;

public record KafkaMessageMetadata(String topic, int partition, long offset) {

    public String deterministicEventId() {
        return "kafka:" + topic + ':' + partition + ':' + offset;
    }
}
