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
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.domain.service.GpsValidationService;
import com.telemetria.domain.service.GsmCompressionService;
import com.telemetria.domain.service.TelemetriaService;
import com.telemetria.infrastructure.integration.routing.OSRMSnapToRoadService;
import com.telemetria.infrastructure.integration.weather.WeatherAlertService;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class TelemetriaKafkaConsumer {

    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoRepository veiculoRepository;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Random random = new Random();
    private final Semaphore semaphore = new Semaphore(10);
    private final AtomicInteger totalProcessados = new AtomicInteger(0);
    private final AtomicInteger totalDescartados = new AtomicInteger(0);

    @Value("${spring.kafka.topic.dlq:telemetria-dlq}")
    private String dlqTopic;

    public TelemetriaKafkaConsumer(
            TelemetriaRepository telemetriaRepository,
            VeiculoRepository veiculoRepository,
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
            KafkaTemplate<String, String> kafkaTemplate) {
        this.telemetriaRepository = telemetriaRepository;
        this.veiculoRepository = veiculoRepository;
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
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "telemetria-raw", groupId = "telemetria-group", concurrency = "3")
    public void processarTelemetria(String mensagem, Acknowledgment ack) {
        long inicio = System.currentTimeMillis();
        int tamanhoOriginal = mensagem.getBytes().length;

        backpressureMonitor.registrarRecebimento("NORMAL");
        if (backpressureMonitor.deveDescartar("NORMAL")) {
            System.out.println("⏭️ Mensagem NORMAL descartada (backpressure CRÍTICO)");
            totalDescartados.incrementAndGet();
            ack.acknowledge();
            return;
        }

        backpressureMonitor.registrarRecebimento();

        System.out.println("📥 [INÍCIO] Processando mensagem do Kafka...");
        System.out.println("📊 Tamanho original: " + tamanhoOriginal + " bytes");
        System.out.println("📊 Lag atual: " + backpressureMonitor.calcularLag() + " mensagens");

        try {
            backpressureMonitor.aplicarBackpressure();

            if (!semaphore.tryAcquire()) {
                System.out.println("⏳ Semáforo ocupado (" + semaphore.getQueueLength() + " threads aguardando)");
                semaphore.acquire();
            }

            try {
                System.out.println("🔄 Convertendo JSON para objeto...");
                JsonNode json = objectMapper.readTree(mensagem);

                Long veiculoId = json.get("vehicle_id").asLong();
                double latitude = json.get("latitude").asDouble();
                double longitude = json.get("longitude").asDouble();

                System.out.println("🔍 ID do veículo extraído: " + veiculoId);
                System.out.println("📍 Coordenadas: " + latitude + ", " + longitude);

                double fatorReducao = criticalAreaService.getFatorReducao(latitude, longitude);

                if (fatorReducao < 1.0) {
                    System.out.println("⚠️ Área crítica detectada! Fator de redução: " + fatorReducao);

                    if (random.nextDouble() > fatorReducao) {
                        System.out.println("⏭️ Mensagem descartada (redução de frequência em área crítica)");
                        criticalAreaService.registrarProcessamento(veiculoId, false);
                        totalDescartados.incrementAndGet();
                        ack.acknowledge();
                        System.out.println("✅ Offset confirmado (mensagem descartada)");

                        if (totalDescartados.get() % 10 == 0) {
                            criticalAreaService.imprimirEstatisticas();
                            imprimirEstatisticasBackpressure();
                        }
                        return;
                    } else {
                        System.out.println("✅ Mensagem selecionada para processamento (aproveitada)");
                    }
                }

                System.out.println("🔎 Buscando veículo no banco de dados...");
                Veiculo veiculo = veiculoRepository.findById(veiculoId)
                        .orElseThrow(() -> new RuntimeException("Veículo não encontrado: " + veiculoId));
                System.out.println("✅ Veículo encontrado: " + veiculo.getPlaca());

                System.out.println("📊 Criando entidade de telemetria...");
                Telemetria telemetria = new Telemetria();
                telemetria.setVeiculo(veiculo);
                telemetria.setLatitude(json.get("latitude").asDouble());
                telemetria.setLongitude(json.get("longitude").asDouble());
                telemetria.setVelocidade(json.get("velocidade").asDouble());

                if (json.has("nivelCombustivel")) {
                    double nivel = json.get("nivelCombustivel").asDouble();
                    telemetria.setNivelCombustivel(nivel);
                    System.out.println("⛽ Nível de combustível: " + nivel + "%");
                }

                if (json.has("hdop")) {
                    telemetria.setHdop(json.get("hdop").asDouble());
                }
                if (json.has("satelites")) {
                    telemetria.setSatelites(json.get("satelites").asInt());
                }
                if (json.has("sinalGsm")) {
                    telemetria.setSinalGsm(json.get("sinalGsm").asDouble());
                }

                if (json.has("timestamp")) {
                    long ts = json.get("timestamp").asLong();
                    telemetria.setDataHora(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(ts), ZoneId.systemDefault()));
                    System.out.println("⏰ Timestamp do veículo: " + ts);
                } else {
                    telemetria.setDataHora(LocalDateTime.now());
                    System.out.println("⏰ Timestamp atual: " + LocalDateTime.now());
                }

                // ✅ RF05 RN-TEL-002: VALIDAÇÃO GPS (antes de qualquer decisão de buffer)
                System.out.println("🛰️ Validando GPS (RN-TEL-002)...");
                Optional<Telemetria> anterior = gpsValidationService.buscarAnterior(
                        veiculoId, telemetria.getDataHora());
                gpsValidationService.validarGps(telemetria, anterior);

                // ✅ RF05 RN-TEL-004: COMPRESSÃO GSM (ANTES DE SALVAR)
                System.out.println("📶 Aplicando política GSM (RN-TEL-004)...");
                boolean processarAgora = gsmService.aplicarPoliticaGsm(telemetria);

                if (!processarAgora) {
                    System.out.println("⏸️ Mensagem armazenada em buffer (sinal fraco). Não será persistida agora.");
                    ack.acknowledge();
                    return;
                }

                // Sinal OK → persistir e continuar
                System.out.println("💾 Salvando telemetria no banco...");
                Telemetria saved = telemetriaRepository.save(telemetria);
                System.out.println("✅ Telemetria salva com ID: " + saved.getId());

                // ✅ RF05 RN-TEL-003: SNAP-TO-ROAD OSRM
                System.out.println("🛣️ Aplicando snap-to-road OSRM (RN-TEL-003)...");
                snapService.snapToRoad(saved.getLatitude(), saved.getLongitude())
                        .ifPresent(snap -> {
                            saved.setLatSnap(snap.latSnap());
                            saved.setLngSnap(snap.lngSnap());
                            saved.setNomeVia(snap.nomeVia());
                            System.out.println("✅ Snap-to-road: " + snap.nomeVia());
                        });

                // ===== ROTEAMENTO POR PRIORIDADE =====
                priorityEventRouter.route(saved, json);

                // 2. Buscar viagem ativa
                System.out.println("🔎 Buscando viagem ativa...");
                var viagemAtiva = viagemRepository.findByVeiculoAndStatus(veiculo, "EM_ANDAMENTO")
                        .orElse(null);

                if (viagemAtiva != null) {
                    System.out.println("✅ Viagem ativa encontrada: " + viagemAtiva.getId());
                } else {
                    System.out.println("ℹ️ Nenhuma viagem ativa no momento");
                }

                // ===== CONSULTA CLIMÁTICA ADAPTATIVA =====
                if (saved.getLatitude() != null && saved.getLongitude() != null) {
                    if (fatorReducao < 1.0 && random.nextDouble() > 0.2) {
                        System.out.println("🌤️ Pulando consulta climática em área crítica (economia de API)");
                    } else {
                        System.out.println("🌦️ Verificando condições climáticas...");
                        weatherAlertService.verificarClimaParaVeiculo(
                                veiculo.getId(),
                                saved.getLatitude(),
                                saved.getLongitude(),
                                viagemAtiva);
                        System.out.println("✅ Verificação climática concluída");
                    }
                }

                // ✅ RF06 RN-POS-001: UPSERT POSIÇÃO ATUAL
                System.out.println("📍 [RF06] Atualizando posição atual do veículo...");
                Boolean ignicao = json.has("ignicao") ? json.get("ignicao").asBoolean() : false;
                
                telemetriaService.atualizarPosicaoAtual(
                    saved.getVeiculo().getId(),
                    saved.getVeiculo().getTenantId(),
                    saved.getVeiculo().getPlaca().replaceAll("-", "").replaceAll("[^A-Z0-9]", ""),
                    saved.getLatitude(),
                    saved.getLongitude(),
                    saved.getVelocidade(),
                    json.has("direcao") ? json.get("direcao").asDouble() : null,
                    ignicao,
                    saved.getDataHora()
                );
                System.out.println("✅ [RF06] Posição atual atualizada: Veículo " + veiculoId);

                // Registrar processamento bem-sucedido
                criticalAreaService.registrarProcessamento(veiculoId, true);
                totalProcessados.incrementAndGet();

                ack.acknowledge();

                long fim = System.currentTimeMillis();
                System.out.println("✅ Offset confirmado (commit) no Kafka");
                System.out.println("✅✅✅ Telemetria processada com SUCESSO: Veículo " + veiculoId +
                        " - ID: " + saved.getId() + " - Tempo: " + (fim - inicio) + "ms");

                backpressureMonitor.registrarProcessamento(fim - inicio);

            } finally {
                semaphore.release();
            }

        } catch (Exception e) {
            System.err.println("❌❌❌ ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();

            try {
                System.out.println("📤 Enviando mensagem para DLQ...");
                kafkaTemplate.send(dlqTopic, mensagem).whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("❌ Erro ao enviar para DLQ: " + ex.getMessage());
                    } else {
                        System.out.println("✅ Mensagem enviada para DLQ com sucesso. Partition: " +
                                result.getRecordMetadata().partition() + ", Offset: " +
                                result.getRecordMetadata().offset());
                    }
                });

                ack.acknowledge();
                System.out.println("✅ Offset confirmado mesmo com erro (DLQ)");

            } catch (Exception dlqEx) {
                System.err.println("❌❌❌ Erro crítico: falha ao enviar para DLQ: " + dlqEx.getMessage());
                dlqEx.printStackTrace();
                System.err.println("⏳ Mensagem NÃO terá commit - será reprocessada");
            }
        }

        if (totalProcessados.get() % 100 == 0) {
            imprimirEstatisticasBackpressure();
        }

        System.out.println("🏁 [FIM] Processamento concluído");
        System.out.println("========================================");
    }

    private void imprimirEstatisticasBackpressure() {
        int lag = backpressureMonitor.calcularLag();
        double taxa = backpressureMonitor.calcularTaxaProcessamento();
        double cpu = backpressureMonitor.getCpuUsage();
        double memory = backpressureMonitor.getMemoryUsage();

        System.out.println("\n📊 ESTATÍSTICAS DE BACKPRESSURE");
        System.out.println("================================");
        System.out.println("📥 Total recebido: " + backpressureMonitor.getMensagensRecebidas());
        System.out.println("✅ Total processado: " + totalProcessados.get());
        System.out.println("⏭️ Total descartado: " + totalDescartados.get());
        System.out.println("⏳ Lag atual: " + lag + " mensagens");
        System.out.println("⚡ Taxa processamento: " + String.format("%.2f", taxa) + " msg/s");
        System.out.println("💻 CPU: " + String.format("%.1f", cpu) + "%");
        System.out.println("🧠 Memória: " + String.format("%.1f", memory) + "%");
        System.out.println("🚦 Backpressure ativo: " + (backpressureMonitor.isBackpressureAtivo() ? "SIM" : "NÃO"));
        System.out.println("🔄 Threads aguardando semáforo: " + semaphore.getQueueLength());

        if (lag > 0 && taxa > 0) {
            long tempoEstimado = (long) (lag / taxa * 1000);
            System.out.println("⏱️ Tempo estimado para recuperação: " + tempoEstimado + "ms");
        }
        System.out.println("================================");
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