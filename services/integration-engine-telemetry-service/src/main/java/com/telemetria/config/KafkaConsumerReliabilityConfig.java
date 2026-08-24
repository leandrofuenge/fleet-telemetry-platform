package com.telemetria.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.infrastructure.metrics.TelemetriaMetrics;

@Configuration
public class KafkaConsumerReliabilityConfig {

    @Bean
    public DefaultErrorHandler telemetriaKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            TelemetriaMetrics metrics,
            @Value("${spring.kafka.topic.telemetria-raw:telemetria-raw}") String rawTopic,
            @Value("${spring.kafka.topic.dlq:telemetria-dlq}") String rawDlqTopic,
            @Value("${spring.kafka.topic.telemetria-events:telemetria-events}") String eventTopic,
            @Value("${spring.kafka.topic.events-dlq:telemetria-events-dlq}") String eventDlqTopic,
            @Value("${telemetria.kafka.retry.max-attempts:5}") int maxAttempts,
            @Value("${telemetria.kafka.retry.initial-interval-ms:1000}") long initialInterval,
            @Value("${telemetria.kafka.retry.multiplier:2.0}") double multiplier,
            @Value("${telemetria.kafka.retry.max-interval-ms:30000}") long maxInterval) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    metrics.incrementarDlq();
                    String topic = rawTopic.equals(record.topic())
                            ? rawDlqTopic
                            : eventTopic.equals(record.topic()) ? eventDlqTopic : record.topic() + "-dlq";
                    // A DLQ pode ter menos partições que o tópico de origem.
                    return new TopicPartition(topic, -1);
                });

        int retries = Math.max(0, maxAttempts - 1);
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retries);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(TelemetriaMessageException.class);
        errorHandler.setCommitRecovered(true);
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (deliveryAttempt > 1) {
                    metrics.incrementarRetries();
                }
            }
        });
        return errorHandler;
    }
}
