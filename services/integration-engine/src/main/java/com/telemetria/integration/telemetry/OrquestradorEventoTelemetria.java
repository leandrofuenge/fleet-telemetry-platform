package com.telemetria.integration.telemetry;

import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.integration.support.AuditLogProcessor;

/** Coordena idempotência, auditoria e encaminhamento Camel de eventos de telemetria. */
@Service
public class OrquestradorEventoTelemetria {

    private final ProcessamentoEventoTelemetriaRepository repository;
    private final ProducerTemplate producerTemplate;

    public OrquestradorEventoTelemetria(
            ProcessamentoEventoTelemetriaRepository repository,
            ProducerTemplate producerTemplate) {
        this.repository = repository;
        this.producerTemplate = producerTemplate;
    }

    @Transactional
    public boolean processar(EventoTelemetriaPersistida event) {
        if (!event.valido()) {
            throw new IllegalArgumentException("Evento de telemetria incompleto para integração.");
        }
        if (repository.existsByEventId(event.eventId())) {
            return false;
        }

        ProcessamentoEventoTelemetria receipt = repository.save(ProcessamentoEventoTelemetria.recebido(event));
        producerTemplate.sendBodyAndHeader(
                RotaEventoTelemetria.ROUTE_PROCESSAR_EVENTO,
                event,
                AuditLogProcessor.HEADER_CORRELATION_ID,
                event.eventId());
        receipt.marcarProcessado();
        return true;
    }
}
