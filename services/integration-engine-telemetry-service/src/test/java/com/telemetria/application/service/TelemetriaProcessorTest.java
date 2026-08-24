package com.telemetria.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.application.service.TelemetriaIdempotencyService.SequenceCheck;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.TelemetriaOutboxEvent;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.infrastructure.messaging.dto.KafkaMessageMetadata;
import com.telemetria.infrastructure.messaging.dto.TelemetriaEnvelope;
import com.telemetria.infrastructure.persistence.TelemetriaOutboxRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoCacheRepository;

@ExtendWith(MockitoExtension.class)
class TelemetriaProcessorTest {

    @Mock private TelemetriaMessageMapper mapper;
    @Mock private TelemetriaIdempotencyService idempotencyService;
    @Mock private TelemetriaQualityService qualityService;
    @Mock private TelemetriaRepository telemetriaRepository;
    @Mock private TelemetriaOutboxRepository outboxRepository;
    @Mock private VeiculoCacheRepository veiculoCacheRepository;

    private TelemetriaProcessor processor;
    private TelemetriaEnvelope envelope;
    private VeiculoCache vehicle;
    private Telemetria telemetry;
    private KafkaMessageMetadata metadata;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        processor = new TelemetriaProcessor(
                mapper, idempotencyService, qualityService, telemetriaRepository,
                outboxRepository, veiculoCacheRepository, objectMapper);
        metadata = new KafkaMessageMetadata("telemetria-raw", 0, 7);
        envelope = new TelemetriaEnvelope(
                objectMapper.createObjectNode(), "{}", 10L, 1L, "evt-7", 3L);
        vehicle = new VeiculoCache();
        vehicle.setId(10L);
        vehicle.setTenantId(1L);
        vehicle.setUuid("00000000-0000-0000-0000-000000000010");
        vehicle.setAtivo(true);
        telemetry = new Telemetria();
        telemetry.setTenantId(1L);
        telemetry.setVeiculoId(10L);
        telemetry.setVeiculoUuid(vehicle.getUuid());
        telemetry.setEventId("evt-7");
        telemetry.setSequenceNumber(3L);
        telemetry.setLatitude(-15.6);
        telemetry.setLongitude(-56.1);
        telemetry.setDataHora(LocalDateTime.of(2026, 8, 17, 10, 0));
    }

    @Test
    void shouldPersistTelemetryAndOutboxInCoreFlow() {
        when(mapper.parse("{}", metadata)).thenReturn(envelope);
        when(veiculoCacheRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(idempotencyService.eventAlreadyProcessed(1L, "evt-7")).thenReturn(false);
        when(mapper.toEntity(envelope, vehicle)).thenReturn(telemetry);
        when(idempotencyService.checkSequence(1L, 10L, null, 3L))
                .thenReturn(new SequenceCheck(false, false, 0, 2L));
        when(telemetriaRepository.saveAndFlush(telemetry)).thenAnswer(invocation -> {
            telemetry.setId(55L);
            return telemetry;
        });

        TelemetriaProcessingResult result = processor.process("{}", metadata);

        assertThat(result.status()).isEqualTo(TelemetriaProcessingResult.Status.PERSISTED);
        assertThat(result.telemetriaId()).isEqualTo(55L);
        assertThat(telemetry.getRecebidoEm()).isNotNull();
        verify(outboxRepository).save(any(TelemetriaOutboxEvent.class));
    }

    @Test
    void shouldStopBeforeMappingWhenEventWasAlreadyProcessed() {
        when(mapper.parse("{}", metadata)).thenReturn(envelope);
        when(veiculoCacheRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(idempotencyService.eventAlreadyProcessed(1L, "evt-7")).thenReturn(true);

        TelemetriaProcessingResult result = processor.process("{}", metadata);

        assertThat(result.status()).isEqualTo(TelemetriaProcessingResult.Status.DUPLICATE_EVENT);
        verify(mapper, never()).toEntity(any(), any());
        verify(telemetriaRepository, never()).saveAndFlush(any());
        verify(outboxRepository, never()).save(any());
    }
}
