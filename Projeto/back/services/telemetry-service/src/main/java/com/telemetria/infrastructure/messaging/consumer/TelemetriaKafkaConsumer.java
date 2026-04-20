package com.telemetria.infrastructure.messaging.consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
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

import com.fasterxml.jackson.core.type.TypeReference;
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
                log.info("[DEBUG] ==================== NOVA MENSAGEM KAFKA ====================");
                log.info("[DEBUG] Mensagem raw recebida (primeiros 1000 chars): {}", mensagem.substring(0, Math.min(1000, mensagem.length())));
                log.trace("🔄 Convertendo JSON para objeto");
                JsonNode json = objectMapper.readTree(mensagem);
                
                // Log todos os campos recebidos
                log.info("[DEBUG] Campos recebidos no JSON: {}", json.fieldNames().next());
                java.util.List<String> camposRecebidos = new java.util.ArrayList<>();
                json.fieldNames().forEachRemaining(camposRecebidos::add);
                log.info("[DEBUG] Total de campos no JSON: {} - Campos: {}", camposRecebidos.size(), camposRecebidos);

                Long veiculoId = json.has("veiculo_id") ? json.get("veiculo_id").asLong() : null;
                Long tenantId = json.has("tenant_id") ? json.get("tenant_id").asLong() : null;
                Long motoristaId = json.has("motorista_id") && !json.get("motorista_id").isNull() ? json.get("motorista_id").asLong() : null;
                Long viagemId = json.has("viagem_id") && !json.get("viagem_id").isNull() ? json.get("viagem_id").asLong() : null;
                String deviceId = json.has("device_id") ? json.get("device_id").asText() : null;
                String imei = json.has("imei_dispositivo") && !json.get("imei_dispositivo").isNull() ? json.get("imei_dispositivo").asText() : null;
                
                double latitude = json.has("latitude") ? json.get("latitude").asDouble() : 0.0;
                double longitude = json.has("longitude") ? json.get("longitude").asDouble() : 0.0;
                double velocidade = json.has("velocidade") ? json.get("velocidade").asDouble() : 0.0;
                
                log.info("[DEBUG] Dados principais: veiculoId={}, tenantId={}, motoristaId={}, viagemId={}", veiculoId, tenantId, motoristaId, viagemId);
                log.info("[DEBUG] Dados device: deviceId={}, imei={}", deviceId, imei);
                log.info("[DEBUG] Dados GPS: lat={}, lon={}, velocidade={}", latitude, longitude, velocidade);

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

                log.info("[DEBUG] Criando entidade Telemetria com {} campos mapeados", camposRecebidos.size());
                Telemetria telemetria = new Telemetria();
                // Não definimos setVeiculo() pois a FK aponta para veiculos_cache
                telemetria.setVeiculoId(veiculo.getId());
                telemetria.setVeiculoUuid(veiculo.getUuid());
                telemetria.setTenantId(veiculo.getTenantId());
                
                // Mapear campos opcionais do JSON
                if (motoristaId != null) {
                    telemetria.setMotoristaId(motoristaId);
                    log.info("[DEBUG] Set motoristaId={}", motoristaId);
                }
                if (viagemId != null) {
                    telemetria.setViagemId(viagemId);
                    log.info("[DEBUG] Set viagemId={}", viagemId);
                }
                if (deviceId != null) {
                    telemetria.setDeviceId(deviceId);
                    log.info("[DEBUG] Set deviceId={}", deviceId);
                }
                if (imei != null) {
                    telemetria.setImeiDispositivo(imei);
                    log.info("[DEBUG] Set imeiDispositivo={}", imei);
                }
                
                telemetria.setLatitude(latitude);
                telemetria.setLongitude(longitude);
                telemetria.setVelocidade(velocidade);

                // Mapear TODOS os campos do JSON
                mapearTodosCampos(telemetria, json);

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
                log.info("[DEBUG] ✅ Telemetria salva com sucesso! ID={}, veiculoId={}, tenantId={}, dataHora={}", 
                    saved.getId(), saved.getVeiculoId(), saved.getTenantId(), saved.getDataHora());
                log.info("[DEBUG] ==================== FIM MENSAGEM ====================\n");

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
            log.error("[DEBUG] Mensagem que causou erro (primeiros 500 chars): {}", mensagem.substring(0, Math.min(500, mensagem.length())));
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

    /**
     * Mapeia TODOS os campos do JSON para a entidade Telemetria
     */
    private void mapearTodosCampos(Telemetria telemetria, JsonNode json) {
        log.info("[DEBUG] Iniciando mapeamento completo de {} campos", json.size());
        int camposMapeados = 0;
        
        // GPS
        if (json.has("altitude")) { telemetria.setAltitude(json.get("altitude").asDouble()); camposMapeados++; }
        if (json.has("direcao")) { telemetria.setDirecao(json.get("direcao").asDouble()); camposMapeados++; }
        if (json.has("hdop")) { telemetria.setHdop(json.get("hdop").asDouble()); camposMapeados++; }
        if (json.has("satelites")) { telemetria.setSatelites(json.get("satelites").asInt()); camposMapeados++; }
        if (json.has("precisao_gps")) { telemetria.setPrecisaoGps(json.get("precisao_gps").asDouble()); camposMapeados++; }
        if (json.has("lat_snap")) { telemetria.setLatSnap(json.get("lat_snap").asDouble()); camposMapeados++; }
        if (json.has("lng_snap")) { telemetria.setLngSnap(json.get("lng_snap").asDouble()); camposMapeados++; }
        if (json.has("nome_via")) { telemetria.setNomeVia(json.get("nome_via").asText()); camposMapeados++; }
        
        // Motor / OBD-II
        if (json.has("ignicao")) { telemetria.setIgnicao(json.get("ignicao").asBoolean()); camposMapeados++; }
        if (json.has("rpm")) { telemetria.setRpm(json.get("rpm").asDouble()); camposMapeados++; }
        if (json.has("carga_motor")) { telemetria.setCargaMotor(json.get("carga_motor").asDouble()); camposMapeados++; }
        if (json.has("torque_motor")) { telemetria.setTorqueMotor(json.get("torque_motor").asDouble()); camposMapeados++; }
        if (json.has("temperatura_motor")) { telemetria.setTemperaturaMotor(json.get("temperatura_motor").asDouble()); camposMapeados++; }
        if (json.has("pressao_oleo")) { telemetria.setPressaoOleo(json.get("pressao_oleo").asDouble()); camposMapeados++; }
        if (json.has("tensao_bateria")) { telemetria.setTensaoBateria(json.get("tensao_bateria").asDouble()); camposMapeados++; }
        if (json.has("odometro")) { telemetria.setOdometro(json.get("odometro").asDouble()); camposMapeados++; }
        if (json.has("horas_motor")) { telemetria.setHorasMotor(json.get("horas_motor").asDouble()); camposMapeados++; }
        if (json.has("aceleracao")) { telemetria.setAceleracao(json.get("aceleracao").asDouble()); camposMapeados++; }
        if (json.has("inclinacao")) { telemetria.setInclinacao(json.get("inclinacao").asDouble()); camposMapeados++; }
        
        // Combustível
        if (json.has("nivel_combustivel")) { telemetria.setNivelCombustivel(json.get("nivel_combustivel").asDouble()); camposMapeados++; }
        if (json.has("consumo_combustivel")) { telemetria.setConsumoCombustivel(json.get("consumo_combustivel").asDouble()); camposMapeados++; }
        if (json.has("consumo_acumulado")) { telemetria.setConsumoAcumulado(json.get("consumo_acumulado").asDouble()); camposMapeados++; }
        if (json.has("tempo_ocioso")) { telemetria.setTempoOcioso(json.get("tempo_ocioso").asInt()); camposMapeados++; }
        if (json.has("tempo_motor_ligado")) { telemetria.setTempoMotorLigado(json.get("tempo_motor_ligado").asInt()); camposMapeados++; }
        
        // Comportamento
        if (json.has("frenagem_brusca")) { telemetria.setFrenagemBrusca(json.get("frenagem_brusca").asBoolean()); camposMapeados++; }
        if (json.has("numero_frenagens")) { telemetria.setNumeroFrenagens(json.get("numero_frenagens").asInt()); camposMapeados++; }
        if (json.has("numero_aceleracoes_bruscas")) { telemetria.setNumeroAceleracoesBruscas(json.get("numero_aceleracoes_bruscas").asInt()); camposMapeados++; }
        if (json.has("excesso_velocidade")) { telemetria.setExcessoVelocidade(json.get("excesso_velocidade").asBoolean()); camposMapeados++; }
        if (json.has("velocidade_limite_via")) { telemetria.setVelocidadeLimiteVia(json.get("velocidade_limite_via").asDouble()); camposMapeados++; }
        if (json.has("curva_brusca")) { telemetria.setCurvaBrusca(json.get("curva_brusca").asBoolean()); camposMapeados++; }
        if (json.has("pontuacao_motorista")) { telemetria.setPontuacaoMotorista(json.get("pontuacao_motorista").asInt()); camposMapeados++; }
        
        // Segurança
        if (json.has("colisao_detectada")) { telemetria.setColisaoDetectada(json.get("colisao_detectada").asBoolean()); camposMapeados++; }
        if (json.has("geofence_violada")) { telemetria.setGeofenceViolada(json.get("geofence_violada").asBoolean()); camposMapeados++; }
        if (json.has("geofence_id")) { 
            if (!json.get("geofence_id").isNull()) { telemetria.setGeofenceId(json.get("geofence_id").asLong()); camposMapeados++; }
        }
        if (json.has("cinto_seguranca")) { telemetria.setCintoSeguranca(json.get("cinto_seguranca").asBoolean()); camposMapeados++; }
        if (json.has("porta_aberta")) { telemetria.setPortaAberta(json.get("porta_aberta").asBoolean()); camposMapeados++; }
        if (json.has("botao_panico")) { telemetria.setBotaoPanico(json.get("botao_panico").asBoolean()); camposMapeados++; }
        if (json.has("adulteracao_gps")) { telemetria.setAdulteracaoGps(json.get("adulteracao_gps").asBoolean()); camposMapeados++; }
        if (json.has("impreciso")) { telemetria.setImpreciso(json.get("impreciso").asBoolean()); camposMapeados++; }
        if (json.has("preservar_dados")) { telemetria.setPreservarDados(json.get("preservar_dados").asBoolean()); camposMapeados++; }
        
        // Carga
        if (json.has("temperatura_carga")) { telemetria.setTemperaturaCarga(json.get("temperatura_carga").asDouble()); camposMapeados++; }
        if (json.has("umidade_carga")) { telemetria.setUmidadeCarga(json.get("umidade_carga").asDouble()); camposMapeados++; }
        if (json.has("peso_carga_kg")) { telemetria.setPesoCargaKg(json.get("peso_carga_kg").asDouble()); camposMapeados++; }
        if (json.has("porta_bau_aberta")) { telemetria.setPortaBauAberta(json.get("porta_bau_aberta").asBoolean()); camposMapeados++; }
        if (json.has("impacto_carga")) { telemetria.setImpactoCarga(json.get("impacto_carga").asBoolean()); camposMapeados++; }
        if (json.has("g_force_impacto")) { telemetria.setGForceImpacto(json.get("g_force_impacto").asDouble()); camposMapeados++; }
        
        // Pneus
        if (json.has("pressao_pneus_json")) { 
            telemetria.setPressaoPneusJson(json.get("pressao_pneus_json").toString()); 
            camposMapeados++; 
        }
        if (json.has("alerta_pneu")) { telemetria.setAlertaPneu(json.get("alerta_pneu").asBoolean()); camposMapeados++; }
        
        // DMS (câmera)
        if (json.has("fadiga_detectada")) { telemetria.setFadigaDetectada(json.get("fadiga_detectada").asBoolean()); camposMapeados++; }
        if (json.has("distracao_detectada")) { telemetria.setDistracaoDetectada(json.get("distracao_detectada").asBoolean()); camposMapeados++; }
        if (json.has("uso_celular_detectado")) { telemetria.setUsoCelularDetectado(json.get("uso_celular_detectado").asBoolean()); camposMapeados++; }
        if (json.has("cigarro_detectado")) { telemetria.setCigarroDetectado(json.get("cigarro_detectado").asBoolean()); camposMapeados++; }
        if (json.has("ausencia_cinto_dms")) { telemetria.setAusenciaCintoDms(json.get("ausencia_cinto_dms").asBoolean()); camposMapeados++; }
        if (json.has("score_dms")) { telemetria.setScoreDms(json.get("score_dms").asInt()); camposMapeados++; }
        
        // Ambiente
        if (json.has("temperatura_externa")) { telemetria.setTemperaturaExterna(json.get("temperatura_externa").asDouble()); camposMapeados++; }
        if (json.has("umidade_externa")) { telemetria.setUmidadeExterna(json.get("umidade_externa").asDouble()); camposMapeados++; }
        if (json.has("chuva_detectada")) { telemetria.setChuvaDetectada(json.get("chuva_detectada").asBoolean()); camposMapeados++; }
        if (json.has("condicao_pista")) { telemetria.setCondicaoPista(json.get("condicao_pista").asText()); camposMapeados++; }
        
        // Conectividade
        if (json.has("sinal_gsm")) { telemetria.setSinalGsm(json.get("sinal_gsm").asDouble()); camposMapeados++; }
        if (json.has("sinal_gps")) { telemetria.setSinalGps(json.get("sinal_gps").asDouble()); camposMapeados++; }
        if (json.has("tecnologia_rede")) { telemetria.setTecnologiaRede(json.get("tecnologia_rede").asText()); camposMapeados++; }
        if (json.has("firmware_versao")) { telemetria.setFirmwareVersao(json.get("firmware_versao").asText()); camposMapeados++; }
        if (json.has("modo_offline")) { telemetria.setModoOffline(json.get("modo_offline").asBoolean()); camposMapeados++; }
        if (json.has("delay_sincronizacao_s")) { telemetria.setDelaySincronizacaoS(json.get("delay_sincronizacao_s").asInt()); camposMapeados++; }
        
        // Tacógrafo
        if (json.has("tacografo_status")) { telemetria.setTacografoStatus(json.get("tacografo_status").asText()); camposMapeados++; }
        if (json.has("tacografo_velocidade")) { telemetria.setTacografoVelocidade(json.get("tacografo_velocidade").asDouble()); camposMapeados++; }
        if (json.has("tacografo_distancia")) { telemetria.setTacografoDistancia(json.get("tacografo_distancia").asDouble()); camposMapeados++; }
        if (json.has("horas_direcao_acumuladas")) { telemetria.setHorasDirecaoAcumuladas(json.get("horas_direcao_acumuladas").asDouble()); camposMapeados++; }
        
        // Manutenção
        if (json.has("manutencao_pendente")) { telemetria.setManutencaoPendente(json.get("manutencao_pendente").asBoolean()); camposMapeados++; }
        if (json.has("proxima_revisao")) {
            if (!json.get("proxima_revisao").isNull()) {
                String proximaRevisaoStr = json.get("proxima_revisao").asText();
                telemetria.setProximaRevisao(LocalDateTime.parse(proximaRevisaoStr));
                camposMapeados++;
            }
        }
        if (json.has("desgaste_freio")) { telemetria.setDesgasteFreio(json.get("desgaste_freio").asDouble()); camposMapeados++; }
        
        // DTC / Payload
        if (json.has("dtc_codes")) { 
            telemetria.setDtcCodes(json.get("dtc_codes").toString()); 
            camposMapeados++; 
        }
        if (json.has("payload")) {
            try {
                Map<String, Object> payloadMap = objectMapper.convertValue(json.get("payload"), new TypeReference<Map<String, Object>>() {});
                telemetria.setPayload(payloadMap);
                camposMapeados++;
            } catch (Exception e) {
                log.warn("[DEBUG] Erro ao converter payload para Map: {}", e.getMessage());
            }
        }
        
        log.info("[DEBUG] Total de campos mapeados: {}", camposMapeados);
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