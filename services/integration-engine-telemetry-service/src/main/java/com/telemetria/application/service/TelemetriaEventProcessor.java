package com.telemetria.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.domain.service.GeofenceService;
import com.telemetria.domain.service.GpsValidationService;
import com.telemetria.domain.service.JornadaService;
import com.telemetria.domain.service.OperacaoService;
import com.telemetria.domain.service.RegraAlertaService;
import com.telemetria.domain.service.TelemetriaService;
import com.telemetria.infrastructure.integration.routing.OSRMSnapToRoadService;
import com.telemetria.infrastructure.integration.routing.OSRMSnapToRoadService.SnapResult;
import com.telemetria.infrastructure.integration.weather.WeatherAlertService;
import com.telemetria.infrastructure.messaging.dto.TelemetriaPersistidaEvent;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoCacheRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;
import com.telemetria.infrastructure.resilience.IntegrationCircuitBreaker;

/** Executa os efeitos derivados sem manter o offset do tópico bruto aberto. */
@Service
public class TelemetriaEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaEventProcessor.class);

    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoCacheRepository veiculoCacheRepository;
    private final ViagemRepository viagemRepository;
    private final GpsValidationService gpsValidationService;
    private final TelemetriaQualityService qualityService;
    private final OSRMSnapToRoadService snapService;
    private final RegraAlertaService regraAlertaService;
    private final OperacaoService operacaoService;
    private final GeofenceService geofenceService;
    private final PriorityEventRouter priorityEventRouter;
    private final WeatherAlertService weatherAlertService;
    private final CriticalAreaService criticalAreaService;
    private final TelemetriaService telemetriaService;
    private final JornadaService jornadaService;
    private final IntegrationCircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    public TelemetriaEventProcessor(
            TelemetriaRepository telemetriaRepository,
            VeiculoCacheRepository veiculoCacheRepository,
            ViagemRepository viagemRepository,
            GpsValidationService gpsValidationService,
            TelemetriaQualityService qualityService,
            OSRMSnapToRoadService snapService,
            RegraAlertaService regraAlertaService,
            OperacaoService operacaoService,
            GeofenceService geofenceService,
            PriorityEventRouter priorityEventRouter,
            WeatherAlertService weatherAlertService,
            CriticalAreaService criticalAreaService,
            TelemetriaService telemetriaService,
            JornadaService jornadaService,
            IntegrationCircuitBreaker circuitBreaker,
            ObjectMapper objectMapper) {
        this.telemetriaRepository = telemetriaRepository;
        this.veiculoCacheRepository = veiculoCacheRepository;
        this.viagemRepository = viagemRepository;
        this.gpsValidationService = gpsValidationService;
        this.qualityService = qualityService;
        this.snapService = snapService;
        this.regraAlertaService = regraAlertaService;
        this.operacaoService = operacaoService;
        this.geofenceService = geofenceService;
        this.priorityEventRouter = priorityEventRouter;
        this.weatherAlertService = weatherAlertService;
        this.criticalAreaService = criticalAreaService;
        this.telemetriaService = telemetriaService;
        this.jornadaService = jornadaService;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;
    }

    public void process(TelemetriaPersistidaEvent event) {
        Telemetria telemetry = telemetriaRepository.findById(event.telemetriaId())
                .orElseThrow(() -> new IllegalStateException("Telemetria da outbox não encontrada: " + event.telemetriaId()));
        if (telemetry.getProcessadoEm() != null) {
            log.info("Evento de enriquecimento duplicado ignorado: {}", event.outboxEventId());
            return;
        }

        VeiculoCache vehicle = veiculoCacheRepository.findById(telemetry.getVeiculoId())
                .orElseThrow(() -> new IllegalStateException("Veículo do evento não encontrado: " + telemetry.getVeiculoId()));
        JsonNode json = parseRawPayload(event.rawPayload());

        Optional<Telemetria> previous = gpsValidationService.buscarAnterior(
                telemetry.getVeiculoId(), telemetry.getDataHora());
        gpsValidationService.validarGps(telemetry, previous);

        Optional<SnapResult> snap = circuitBreaker.execute("osrm-snap", () -> {
            Optional<SnapResult> result = snapService.snapToRoad(telemetry.getLatitude(), telemetry.getLongitude());
            if (result.isEmpty()) throw new IllegalStateException("OSRM não retornou ponto de via");
            return result;
        }, Optional::empty);
        snap.ifPresent(value -> {
            telemetry.setLatSnap(value.latSnap());
            telemetry.setLngSnap(value.lngSnap());
            telemetry.setNomeVia(value.nomeVia());
        });

        qualityService.evaluate(telemetry);
        telemetriaRepository.save(telemetry);

        regraAlertaService.avaliar(telemetry);
        if (Boolean.TRUE.equals(telemetry.getColisaoDetectada())
                || Boolean.TRUE.equals(telemetry.getBotaoPanico())) {
            operacaoService.abrirPorTelemetria(telemetry);
        }
        geofenceService.verificarGeofences(telemetry);
        priorityEventRouter.route(telemetry, json);

        var activeTrip = viagemRepository.findByVeiculoIdAndStatus(vehicle.getId(), "EM_ANDAMENTO")
                .orElse(null);
        double reductionFactor = criticalAreaService.getFatorReducao(
                telemetry.getLatitude(), telemetry.getLongitude());
        if (reductionFactor >= 1.0) {
            circuitBreaker.run("weather", () -> weatherAlertService.verificarClimaParaVeiculo(
                    vehicle.getId(), telemetry.getLatitude(), telemetry.getLongitude(), activeTrip));
        }

        telemetriaService.atualizarPosicaoAtual(
                vehicle.getId(),
                vehicle.getTenantId(),
                normalizePlate(vehicle.getPlaca()),
                telemetry.getLatitude(),
                telemetry.getLongitude(),
                telemetry.getVelocidade(),
                telemetry.getDirecao(),
                telemetry.getIgnicao(),
                telemetry.getDataHora());

        if (telemetry.getMotoristaId() != null) {
            boolean driving = Boolean.TRUE.equals(telemetry.getIgnicao())
                    && telemetry.getVelocidade() != null && telemetry.getVelocidade() > 1.0;
            jornadaService.registrarDirecao(
                    telemetry.getTenantId(), telemetry.getMotoristaId(), telemetry.getVeiculoId(),
                    telemetry.getViagemId(), driving, telemetry.getDataHora());
        }

        telemetry.setProcessadoEm(LocalDateTime.now());
        telemetriaRepository.save(telemetry);
        criticalAreaService.registrarProcessamento(vehicle.getId(), true);
    }

    private JsonNode parseRawPayload(String rawPayload) {
        try {
            JsonNode json = objectMapper.readTree(rawPayload);
            if (json == null || !json.isObject()) {
                throw new TelemetriaMessageException("Payload original do evento é inválido");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new TelemetriaMessageException("Payload original do evento é inválido", e);
        }
    }

    private String normalizePlate(String plate) {
        return plate == null ? "" : plate.replaceAll("-", "").replaceAll("[^A-Z0-9]", "");
    }
}
