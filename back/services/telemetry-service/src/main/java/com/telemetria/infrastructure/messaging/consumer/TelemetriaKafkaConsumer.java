package com.telemetria.infrastructure.messaging.consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.application.service.BackpressureMonitorService;
import com.telemetria.application.service.CriticalAreaService;
import com.telemetria.application.service.PriorityEventRouter;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.domain.service.GeofenceService;
import com.telemetria.domain.service.GpsValidationService;
import com.telemetria.domain.service.GsmCompressionService;
import com.telemetria.domain.service.TelemetriaService;
import com.telemetria.infrastructure.integration.routing.OSRMSnapToRoadService;
import com.telemetria.infrastructure.integration.weather.WeatherAlertService;
import com.telemetria.infrastructure.metrics.TelemetriaMetrics;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoCacheRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

import io.micrometer.core.instrument.Timer;

@Service
public class TelemetriaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaKafkaConsumer.class);

    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoCacheRepository veiculoCacheRepository;
    private final ViagemRepository viagemRepository;
    private final AlertaService alertaService;
    private final WeatherAlertService weatherAlertService;
    private final CriticalAreaService criticalAreaService;
    private final BackpressureMonitorService backpressureMonitor;
    private final GpsValidationService gpsValidationService;
    private final OSRMSnapToRoadService snapService;
    private final GsmCompressionService gsmService;
    private final TelemetriaService telemetriaService;
    private final PriorityEventRouter priorityEventRouter;
    private final GeofenceService geofenceService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TelemetriaMetrics metrics;

    private final Random random = new Random();
    private final Semaphore semaphore = new Semaphore(100); // AUMENTADO de 10 para 100
    private final AtomicInteger totalProcessados = new AtomicInteger(0);
    private final AtomicInteger totalDescartados = new AtomicInteger(0);

    @Value("${spring.kafka.topic.dlq:telemetria-dlq}")
    private String dlqTopic;

    public TelemetriaKafkaConsumer(
            TelemetriaRepository telemetriaRepository,
            VeiculoCacheRepository veiculoCacheRepository,
            ViagemRepository viagemRepository,
            AlertaService alertaService,
            WeatherAlertService weatherAlertService,
            CriticalAreaService criticalAreaService,
            BackpressureMonitorService backpressureMonitor,
            GpsValidationService gpsValidationService,
            OSRMSnapToRoadService snapService,
            GsmCompressionService gsmService,
            TelemetriaService telemetriaService,
            PriorityEventRouter priorityEventRouter,
            GeofenceService geofenceService,
            KafkaTemplate<String, String> kafkaTemplate,
            TelemetriaMetrics metrics) {
        this.telemetriaRepository = telemetriaRepository;
        this.veiculoCacheRepository = veiculoCacheRepository;
        this.viagemRepository = viagemRepository;
        this.alertaService = alertaService;
        this.weatherAlertService = weatherAlertService;
        this.criticalAreaService = criticalAreaService;
        this.backpressureMonitor = backpressureMonitor;
        this.gpsValidationService = gpsValidationService;
        this.snapService = snapService;
        this.gsmService = gsmService;
        this.telemetriaService = telemetriaService;
        this.priorityEventRouter = priorityEventRouter;
        this.geofenceService = geofenceService;
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    @KafkaListener(topics = "telemetria-raw", groupId = "telemetria-group", concurrency = "8")
    public void processarTelemetria(String mensagem, Acknowledgment ack) {
        Timer.Sample sample = metrics.iniciarTimer();
        long inicio = System.currentTimeMillis();
        int tamanhoOriginal = mensagem.getBytes().length;

        // ✅ CORRIGIDO: Registrar recebimento APENAS UMA VEZ
        backpressureMonitor.registrarRecebimento();

        // Verificar se deve descartar por backpressure
        if (backpressureMonitor.deveDescartar("NORMAL")) {
            log.debug("⏭️ Mensagem NORMAL descartada (backpressure CRÍTICO)");
            totalDescartados.incrementAndGet();
            metrics.incrementarDescartadas();
            ack.acknowledge();
            metrics.pararTimer(sample);
            return;
        }

        // ✅ REMOVIDO: backpressureMonitor.registrarRecebimento() duplicado

        log.debug("📥 Processando mensagem. Tamanho: {}, Lag: {}", 
            tamanhoOriginal, backpressureMonitor.calcularLag());

        try {
            backpressureMonitor.aplicarBackpressure();

            if (!semaphore.tryAcquire()) {
                log.debug("⏳ Semáforo ocupado ({} threads aguardando)", semaphore.getQueueLength());
                semaphore.acquire();
            }

            try {
                log.trace("🔄 Convertendo JSON para objeto");
                JsonNode json = objectMapper.readTree(mensagem);

                Long veiculoId = json.get("veiculo_id").asLong();
                double latitude = json.get("latitude").asDouble();
                double longitude = json.get("longitude").asDouble();

                log.trace("🔍 Veículo: {}, Coords: {}, {}", veiculoId, latitude, longitude);

                double fatorReducao = criticalAreaService.getFatorReducao(latitude, longitude);

                if (fatorReducao < 1.0) {
                    log.debug("⚠️ Área crítica detectada. Fator: {}", fatorReducao);

                    if (random.nextDouble() > fatorReducao) {
                        log.debug("⏭️ Mensagem descartada (redução em área crítica)");
                        criticalAreaService.registrarProcessamento(veiculoId, false);
                        totalDescartados.incrementAndGet();
                        metrics.incrementarDescartadas();
                        ack.acknowledge();
                        metrics.pararTimer(sample);
                        return;
                    } else {
                        log.trace("✅ Mensagem aproveitada");
                    }
                }

                log.trace("🔎 Buscando veículo no banco");
                VeiculoCache veiculo = veiculoCacheRepository.findById(veiculoId)
                        .orElseThrow(() -> new RuntimeException("Veículo não encontrado no cache: " + veiculoId));
                log.trace("✅ Veículo encontrado: {}", veiculo.getPlaca());

                log.trace("📊 Criando entidade de telemetria");
                Telemetria telemetria = new Telemetria();
                // Não definimos setVeiculo() pois a FK aponta para veiculos_cache
                telemetria.setVeiculoId(veiculo.getId());
                telemetria.setVeiculoUuid(veiculo.getUuid());
                telemetria.setTenantId(veiculo.getTenantId());
                telemetria.setLatitude(json.get("latitude").asDouble());
                telemetria.setLongitude(json.get("longitude").asDouble());
                telemetria.setVelocidade(json.get("velocidade").asDouble());

                if (json.has("nivel_combustivel")) {
                    double nivel = json.get("nivel_combustivel").asDouble();
                    telemetria.setNivelCombustivel(nivel);
                    log.trace("⛽ Combustível: {}%", nivel);
                }

                if (json.has("hdop")) {
                    telemetria.setHdop(json.get("hdop").asDouble());
                }
                if (json.has("satelites")) {
                    telemetria.setSatelites(json.get("satelites").asInt());
                }
                if (json.has("sinal_gsm")) {
                    telemetria.setSinalGsm(json.get("sinal_gsm").asDouble());
                }

                if (json.has("timestamp")) {
                    long ts = json.get("timestamp").asLong();
                    telemetria.setDataHora(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
                } else {
                    telemetria.setDataHora(LocalDateTime.now());
                }

                // RF05 RN-TEL-002: VALIDAÇÃO GPS
                Optional<Telemetria> anterior = gpsValidationService.buscarAnterior(
                        veiculoId, telemetria.getDataHora());
                gpsValidationService.validarGps(telemetria, anterior);

                // RF05 RN-TEL-004: COMPRESSÃO GSM
                log.trace("📶 Aplicando política GSM");
                boolean processarAgora = gsmService.aplicarPoliticaGsm(telemetria);

                if (!processarAgora) {
                    log.debug("⏸️ Mensagem em buffer (sinal fraco)");
                    metrics.incrementarDescartadas();
                    ack.acknowledge();
                    metrics.pararTimer(sample);
                    return;
                }

                log.trace("💾 Salvando telemetria");
                Telemetria saved = telemetriaRepository.save(telemetria);
                log.trace("✅ Telemetria salva: {}", saved.getId());

                // ✅ RF05 RN-TEL-003: SNAP-TO-ROAD OSRM
                log.trace("🛣️ Snap-to-road");
                snapService.snapToRoad(saved.getLatitude(), saved.getLongitude())
                        .ifPresent(snap -> {
                            saved.setLatSnap(snap.latSnap());
                            saved.setLngSnap(snap.lngSnap());
                            saved.setNomeVia(snap.nomeVia());
                            log.trace("✅ Snap: {}", snap.nomeVia());
                        });

                // ===== GEOFENCE (RF07) =====
                geofenceService.verificarGeofences(saved);

                // ===== ROTEAMENTO POR PRIORIDADE =====
                priorityEventRouter.route(saved, json);

                // Buscar viagem ativa
                log.trace(" Buscando viagem ativa");
                var viagemAtiva = viagemRepository.findByVeiculoIdAndStatus(veiculo.getId(), "EM_ANDAMENTO")
                        .orElse(null);

                if (viagemAtiva != null) {
                    log.trace(" Viagem ativa: {}", viagemAtiva.getId());
                }

                // ===== CONSULTA CLIMÁTICA ADAPTATIVA =====
                if (saved.getLatitude() != null && saved.getLongitude() != null) {
                    if (fatorReducao < 1.0 && random.nextDouble() > 0.2) {
                        log.trace("🌤️ Pulando consulta climática (área crítica)");
                    } else {
                        log.trace("🌦️ Verificando clima");
                        weatherAlertService.verificarClimaParaVeiculo(
                                veiculo.getId(),
                                saved.getLatitude(),
                                saved.getLongitude(),
                                viagemAtiva);
                    }
                }

                // ✅ RF06 RN-POS-001: UPSERT POSIÇÃO ATUAL
                log.trace("📍 Atualizando posição atual");
                Boolean ignicao = json.has("ignicao") ? json.get("ignicao").asBoolean() : false;
                
                telemetriaService.atualizarPosicaoAtual(
                    veiculo.getId(),
                    veiculo.getTenantId(),
                    veiculo.getPlaca().replaceAll("-", "").replaceAll("[^A-Z0-9]", ""),
                    saved.getLatitude(),
                    saved.getLongitude(),
                    saved.getVelocidade(),
                    json.has("direcao") ? json.get("direcao").asDouble() : null,
                    ignicao,
                    saved.getDataHora()
                );

                // Registrar processamento bem-sucedido
                criticalAreaService.registrarProcessamento(veiculoId, true);
                totalProcessados.incrementAndGet();
                metrics.incrementarProcessadas();

                ack.acknowledge();

                long fim = System.currentTimeMillis();
                long tempoProcessamento = fim - inicio;
                
                // ✅ Log consolidado
                log.debug("✅ Telemetria processada: Veículo={}, ID={}, Tempo={}ms", 
                    veiculoId, saved.getId(), tempoProcessamento);

                backpressureMonitor.registrarProcessamento(tempoProcessamento);

            } finally {
                semaphore.release();
            }

        } catch (Exception e) {
            log.error("❌ ERRO no processamento: {}", e.getMessage(), e);
            metrics.incrementarDescartadas();
            try {
                log.debug("📤 Enviando para DLQ");
                kafkaTemplate.send(dlqTopic, mensagem).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ Erro ao enviar para DLQ: {}", ex.getMessage());
                    } else {
                        log.debug("✅ DLQ enviado. Partition: {}, Offset: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

                ack.acknowledge();

            } catch (Exception dlqEx) {
                log.error("❌ Falha crítica ao enviar para DLQ: {}", dlqEx.getMessage(), dlqEx);
            }
        } finally {
            metrics.pararTimer(sample);
        }

        // Imprimir estatísticas a cada 100 mensagens
        if (totalProcessados.get() % 100 == 0) {
            imprimirEstatisticasBackpressure();
        }

        log.trace("🏁 Processamento concluído");
    }

    private void imprimirEstatisticasBackpressure() {
        int lag = backpressureMonitor.calcularLag();
        double taxa = backpressureMonitor.calcularTaxaProcessamento();
        double cpu = backpressureMonitor.getCpuUsage();
        double memory = backpressureMonitor.getMemoryUsage();

        log.info("📊 ESTATÍSTICAS - Recebido: {}, Processado: {}, Descartado: {}, Lag: {}, Taxa: {:.2f} msg/s, CPU: {:.1f}%, Memória: {:.1f}%, Backpressure: {}, Semáforo: {}",
                backpressureMonitor.getMensagensRecebidas(),
                totalProcessados.get(),
                totalDescartados.get(),
                lag,
                taxa,
                cpu,
                memory,
                backpressureMonitor.isBackpressureAtivo() ? "SIM" : "NÃO",
                semaphore.getQueueLength());

        if (lag > 0 && taxa > 0) {
            long tempoEstimado = (long) (lag / taxa * 1000);
            log.info("⏱️ Tempo estimado para recuperação: {}ms", tempoEstimado);
        }
    }

    public static byte[] comprimirGzip(String dados) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
                gzip.write(dados.getBytes("UTF-8"));
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comprimir dados", e);
        }
    }

    public static String descomprimirGzip(byte[] dadosComprimidos) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(dadosComprimidos);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(bis)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
            }
            return bos.toString("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descomprimir dados", e);
        }
    }
}