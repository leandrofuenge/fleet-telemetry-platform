package com.app.telemetria.infrastructure.messaging.consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

import com.app.telemetria.application.service.BackpressureMonitorService;
import com.app.telemetria.application.service.CriticalAreaService;
import com.app.telemetria.domain.entity.Telemetria;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.service.AlertaService;
import com.app.telemetria.infrastructure.integration.weather.WeatherAlertService;
import com.app.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;
import com.app.telemetria.infrastructure.persistence.ViagemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TelemetriaKafkaConsumer {

    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemRepository viagemRepository;
    private final AlertaService alertaService;
    private final WeatherAlertService weatherAlertService;
    private final CriticalAreaService criticalAreaService;
    private final BackpressureMonitorService backpressureMonitor;
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
            KafkaTemplate<String, String> kafkaTemplate) {
        this.telemetriaRepository = telemetriaRepository;
        this.veiculoRepository = veiculoRepository;
        this.viagemRepository = viagemRepository;
        this.alertaService = alertaService;
        this.weatherAlertService = weatherAlertService;
        this.criticalAreaService = criticalAreaService;
        this.backpressureMonitor = backpressureMonitor;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Comprime dados usando GZIP
     */
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

    /**
     * Descomprime dados GZIP
     */
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

    @KafkaListener(topics = "telemetria-raw", groupId = "telemetria-group", concurrency = "3")
    public void processarTelemetria(String mensagem, Acknowledgment ack) {
        long inicio = System.currentTimeMillis();
        int tamanhoOriginal = mensagem.getBytes().length;

        // ===== NOVO: Registrar recebimento no monitor de backpressure =====
        backpressureMonitor.registrarRecebimento();

        System.out.println("📥 [INÍCIO] Processando mensagem do Kafka...");
        System.out.println("📊 Tamanho original: " + tamanhoOriginal + " bytes");
        System.out.println("📊 Lag atual: " + backpressureMonitor.calcularLag() + " mensagens"); // NOVO

        try {
            // ===== NOVO: Aplicar backpressure baseado em CPU/memória/lag =====
            backpressureMonitor.aplicarBackpressure();

            // ===== NOVO: Controle de concorrência com semáforo =====
            if (!semaphore.tryAcquire()) {
                System.out.println("⏳ Semáforo ocupado (" + semaphore.getQueueLength() + " threads aguardando)");
                semaphore.acquire(); // Bloqueia até conseguir
            }

            try {
                // Converter JSON para objeto
                System.out.println("🔄 Convertendo JSON para objeto...");
                JsonNode json = objectMapper.readTree(mensagem);

                Long veiculoId = json.get("vehicle_id").asLong();
                double latitude = json.get("latitude").asDouble();
                double longitude = json.get("longitude").asDouble();

                System.out.println("🔍 ID do veículo extraído: " + veiculoId);
                System.out.println("📍 Coordenadas: " + latitude + ", " + longitude);

                // ===== VERIFICAÇÃO DE ÁREA CRÍTICA =====
                double fatorReducao = criticalAreaService.getFatorReducao(latitude, longitude);

                if (fatorReducao < 1.0) {
                    System.out.println("⚠️ Área crítica detectada! Fator de redução: " + fatorReducao);

                    // Decidir se processa baseado no fator de redução
                    if (random.nextDouble() > fatorReducao) {
                        System.out.println("⏭️  Mensagem descartada (redução de frequência em área crítica)");
                        criticalAreaService.registrarProcessamento(veiculoId, false);
                        totalDescartados.incrementAndGet(); // NOVO

                        // Commit do offset mesmo descartando
                        ack.acknowledge();
                        System.out.println("✅ Offset confirmado (mensagem descartada)");

                        // Imprimir estatísticas
                        if (totalDescartados.get() % 10 == 0) {
                            criticalAreaService.imprimirEstatisticas();
                            imprimirEstatisticasBackpressure(); // NOVO
                        }

                        return; // Sai do método sem processar
                    } else {
                        System.out.println("✅ Mensagem selecionada para processamento (aproveitada)");
                    }
                }

                // Buscar veículo no banco
                System.out.println("🔎 Buscando veículo no banco de dados...");
                Veiculo veiculo = veiculoRepository.findById(veiculoId)
                        .orElseThrow(() -> new RuntimeException("Veículo não encontrado: " + veiculoId));
                System.out.println("✅ Veículo encontrado: " + veiculo.getPlaca());

                // Criar entidade Telemetria
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

                // Timestamp do veículo ou atual
                if (json.has("timestamp")) {
                    long ts = json.get("timestamp").asLong();
                    telemetria.setDataHora(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(ts), ZoneId.systemDefault()));
                    System.out.println("⏰ Timestamp do veículo: " + ts);
                } else {
                    telemetria.setDataHora(LocalDateTime.now());
                    System.out.println("⏰ Timestamp atual: " + LocalDateTime.now());
                }

                // 1. Salvar no banco
                System.out.println("💾 Salvando telemetria no banco...");
                Telemetria saved = telemetriaRepository.save(telemetria);
                System.out.println("✅ Telemetria salva com ID: " + saved.getId());

                // 2. Buscar viagem ativa
                System.out.println("🔎 Buscando viagem ativa...");
                var viagemAtiva = viagemRepository.findByVeiculoAndStatus(veiculo, "EM_ANDAMENTO")
                        .orElse(null);

                if (viagemAtiva != null) {
                    System.out.println("✅ Viagem ativa encontrada: " + viagemAtiva.getId());
                } else {
                    System.out.println("ℹ️ Nenhuma viagem ativa no momento");
                }

                // 3. Gerar alertas de telemetria
                System.out.println("🚨 Gerando alertas de telemetria...");
                alertaService.processarTelemetria(saved);
                System.out.println("✅ Alertas processados");

                // ===== CONSULTA CLIMÁTICA ADAPTATIVA =====
                if (telemetria.getLatitude() != null && telemetria.getLongitude() != null) {
                    // Em áreas críticas, só consulta clima em 20% das vezes
                    if (fatorReducao < 1.0 && random.nextDouble() > 0.2) {
                        System.out.println("🌤️ Pulando consulta climática em área crítica (economia de API)");
                    } else {
                        System.out.println("🌦️ Verificando condições climáticas...");
                        weatherAlertService.verificarClimaParaVeiculo(
                                veiculo.getId(),
                                telemetria.getLatitude(),
                                telemetria.getLongitude(),
                                viagemAtiva);
                        System.out.println("✅ Verificação climática concluída");
                    }
                }

                // Registrar processamento bem-sucedido
                criticalAreaService.registrarProcessamento(veiculoId, true);
                totalProcessados.incrementAndGet(); // NOVO

                // Commit manual do offset
                ack.acknowledge();

                long fim = System.currentTimeMillis();
                System.out.println("✅ Offset confirmado (commit) no Kafka");
                System.out.println("✅✅✅ Telemetria processada com SUCESSO: Veículo " + veiculoId +
                        " - ID: " + saved.getId() + " - Tempo: " + (fim - inicio) + "ms");

                // ===== NOVO: Registrar processamento no monitor =====
                backpressureMonitor.registrarProcessamento(fim - inicio);

            } finally {
                // ===== NOVO: Liberar semáforo sempre =====
                semaphore.release();
            }

        } catch (Exception e) {
            System.err.println("❌❌❌ ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();

            // Envia para Dead Letter Queue
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

        // ===== NOVO: Imprimir estatísticas a cada 100 mensagens =====
        if (totalProcessados.get() % 100 == 0) {
            imprimirEstatisticasBackpressure();
        }

        System.out.println("🏁 [FIM] Processamento concluído");
        System.out.println("========================================");
    }

    // ===== NOVO: Método para imprimir estatísticas de backpressure =====
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
}
