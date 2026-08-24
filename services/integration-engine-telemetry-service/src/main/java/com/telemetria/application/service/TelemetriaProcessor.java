package com.telemetria.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.application.service.TelemetriaIdempotencyService.SequenceCheck;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.TelemetriaOutboxEvent;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.infrastructure.messaging.dto.KafkaMessageMetadata;
import com.telemetria.infrastructure.messaging.dto.TelemetriaEnvelope;
import com.telemetria.infrastructure.messaging.dto.TelemetriaPersistidaEvent;
import com.telemetria.infrastructure.persistence.TelemetriaOutboxRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoCacheRepository;

@Service
public class TelemetriaProcessor {

    private final TelemetriaMessageMapper mapper;
    private final TelemetriaIdempotencyService idempotencyService;
    private final TelemetriaQualityService qualityService;
    private final TelemetriaRepository telemetriaRepository;
    private final TelemetriaOutboxRepository outboxRepository;
    private final VeiculoCacheRepository veiculoCacheRepository;
    private final ObjectMapper objectMapper;

    public TelemetriaProcessor(
            TelemetriaMessageMapper mapper,
            TelemetriaIdempotencyService idempotencyService,
            TelemetriaQualityService qualityService,
            TelemetriaRepository telemetriaRepository,
            TelemetriaOutboxRepository outboxRepository,
            VeiculoCacheRepository veiculoCacheRepository,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.qualityService = qualityService;
        this.telemetriaRepository = telemetriaRepository;
        this.outboxRepository = outboxRepository;
        this.veiculoCacheRepository = veiculoCacheRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TelemetriaProcessingResult process(String payload, KafkaMessageMetadata metadata) {
        TelemetriaEnvelope envelope = mapper.parse(payload, metadata);
        VeiculoCache veiculo = veiculoCacheRepository.findById(envelope.veiculoId())
                .orElseThrow(() -> new TelemetriaMessageException(
                        "Veículo não encontrado no cache: " + envelope.veiculoId()));

        if (!Boolean.TRUE.equals(veiculo.getAtivo())) {
            throw new TelemetriaMessageException("Veículo inativo: " + envelope.veiculoId());
        }
        if (envelope.tenantIdInformado() != null
                && !envelope.tenantIdInformado().equals(veiculo.getTenantId())) {
            throw new TelemetriaMessageException("tenant_id não pertence ao veículo informado");
        }

        if (idempotencyService.eventAlreadyProcessed(veiculo.getTenantId(), envelope.eventId())) {
            return new TelemetriaProcessingResult(
                    TelemetriaProcessingResult.Status.DUPLICATE_EVENT,
                    null,
                    envelope.eventId(),
                    veiculo.getId(),
                    false,
                    0);
        }

        Telemetria telemetria = mapper.toEntity(envelope, veiculo);
        SequenceCheck sequence = idempotencyService.checkSequence(
                veiculo.getTenantId(), veiculo.getId(), telemetria.getDeviceId(), telemetria.getSequenceNumber());
        if (sequence.duplicate()) {
            return new TelemetriaProcessingResult(
                    TelemetriaProcessingResult.Status.DUPLICATE_SEQUENCE,
                    null,
                    envelope.eventId(),
                    veiculo.getId(),
                    false,
                    0);
        }

        telemetria.setForaDeOrdem(sequence.outOfOrder());
        telemetria.setSequenceGap(sequence.gap());
        telemetria.setRecebidoEm(LocalDateTime.now());
        qualityService.evaluate(telemetria);

        Telemetria saved = telemetriaRepository.saveAndFlush(telemetria);
        TelemetriaOutboxEvent outbox = TelemetriaOutboxEvent.pending(saved, "{}");
        outbox.setPayload(serializeEvent(outbox, saved, envelope.rawPayload()));
        outboxRepository.save(outbox);

        return new TelemetriaProcessingResult(
                TelemetriaProcessingResult.Status.PERSISTED,
                saved.getId(),
                saved.getEventId(),
                saved.getVeiculoId(),
                sequence.outOfOrder(),
                sequence.gap());
    }

    private String serializeEvent(TelemetriaOutboxEvent outbox, Telemetria telemetry, String rawPayload) {
        TelemetriaPersistidaEvent event = new TelemetriaPersistidaEvent(
                outbox.getId(),
                telemetry.getEventId(),
                telemetry.getId(),
                telemetry.getTenantId(),
                telemetry.getVeiculoId(),
                telemetry.getDataHora(),
                telemetry.getRecebidoEm(),
                rawPayload);
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento de outbox", e);
        }
    }
}
