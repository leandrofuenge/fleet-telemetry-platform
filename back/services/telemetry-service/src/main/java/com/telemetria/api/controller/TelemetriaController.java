// =====================================================================
// TelemetriaController.java  —  chamadas ao repository ajustadas
// =====================================================================
package com.telemetria.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.infrastructure.integration.weather.WeatherAlertService;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@RestController
@RequestMapping("/api/v1/telemetria")
public class TelemetriaController {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaController.class);

    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoRepository veiculoRepository;
    private final KafkaTemplate<String, TelemetriaRequest> kafkaTemplate;

    private static final String TOPIC = "telemetria-raw";

    public TelemetriaController(
            TelemetriaRepository telemetriaRepository,
            VeiculoRepository veiculoRepository,
            ViagemRepository viagemRepository,
            AlertaService alertaService,
            WeatherAlertService weatherAlertService,
            KafkaTemplate<String, TelemetriaRequest> kafkaTemplate) {
        this.telemetriaRepository = telemetriaRepository;
        this.veiculoRepository = veiculoRepository;
        this.kafkaTemplate = kafkaTemplate;
        log.info("✅ TelemetriaController inicializado");
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody TelemetriaRequest request) {
        log.info("📡 Requisição de telemetria recebida");

        if (request.getVeiculo() == null || request.getVeiculo().getId() == null) {
            log.error("❌ ID do veículo não informado");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID do veículo é obrigatório");
        }

        Veiculo veiculo = veiculoRepository.findById(request.getVeiculo().getId())
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", request.getVeiculo().getId());
                    return new BusinessException(
                            ErrorCode.VEICULO_NOT_FOUND,
                            request.getVeiculo().getId().toString());
                });

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Latitude e longitude são obrigatórios");
        }

        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Latitude deve estar entre -90 e 90");
        }

        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Longitude deve estar entre -180 e 180");
        }

        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC, veiculo.getId().toString(), request)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.debug("✅ Mensagem enviada - Offset: {}, Partição: {}",
                                        result.getRecordMetadata().offset(),
                                        result.getRecordMetadata().partition());
                            } else {
                                log.error("❌ Erro ao enviar para Kafka: {}", ex.getMessage(), ex);
                            }
                        });
            } catch (Exception e) {
                log.error("❌ Erro no processamento assíncrono: {}", e.getMessage(), e);
            }
        });

        log.info("✅ Telemetria aceita para processamento - Veículo ID: {}", veiculo.getId());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Telemetria recebida e em processamento. ID do veículo: " + veiculo.getId());
    }

    @GetMapping("/veiculo/{veiculoId}")
    public List<Telemetria> listarPorVeiculo(@PathVariable Long veiculoId) {
        log.info("📋 Listando telemetrias para veículo ID: {}", veiculoId);

        // Valida existência do veículo antes de consultar
        veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", veiculoId);
                    return new BusinessException(ErrorCode.VEICULO_NOT_FOUND, veiculoId.toString());
                });

        // Passa apenas o ID — repository já usa native query com Long
        List<Telemetria> telemetrias = telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId);

        log.info("✅ Encontradas {} telemetrias para veículo {}", telemetrias.size(), veiculoId);
        return telemetrias;
    }

    @GetMapping("/veiculo/{veiculoId}/ultima")
    public ResponseEntity<Telemetria> ultimaTelemetria(@PathVariable Long veiculoId) {
        log.info("🔍 Buscando última telemetria para veículo ID: {}", veiculoId);

        veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", veiculoId);
                    return new BusinessException(ErrorCode.VEICULO_NOT_FOUND, veiculoId.toString());
                });

        // Passa apenas o ID — sem necessidade de instanciar Veiculo
        return telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId)
                .map(telemetria -> {
                    log.info("✅ Última telemetria - Data: {}, Velocidade: {}",
                            telemetria.getDataHora(), telemetria.getVelocidade());
                    return ResponseEntity.ok(telemetria);
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Nenhuma telemetria encontrada para veículo {}", veiculoId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/veiculo/{veiculoId}/periodo")
    public List<Telemetria> listarPorPeriodo(
            @PathVariable Long veiculoId,
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) {

        log.info("📅 Buscando telemetrias do veículo {} entre {} e {}", veiculoId, inicio, fim);

        if (inicio.isAfter(fim)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Data de início não pode ser posterior à data de fim");
        }

        veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", veiculoId);
                    return new BusinessException(ErrorCode.VEICULO_NOT_FOUND, veiculoId.toString());
                });

        // Passa apenas o ID — sem necessidade de instanciar Veiculo
        List<Telemetria> telemetrias = telemetriaRepository
                .findByVeiculoIdAndDataHoraBetween(veiculoId, inicio, fim);

        log.info("✅ {} telemetrias encontradas para veículo {} no período", telemetrias.size(), veiculoId);
        return telemetrias;
    }

    @GetMapping("/debug/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Telemetria service está operacional");
    }

    @GetMapping("/debug/kafka")
    public ResponseEntity<String> testarKafka() {
        log.info("🔍 Testando conexão com Kafka");

        TelemetriaRequest testRequest = new TelemetriaRequest();
        testRequest.setLatitude(-23.5505);
        testRequest.setLongitude(-46.6333);
        testRequest.setVelocidade(60.0);
        testRequest.setDataHora(LocalDateTime.now());

        VeiculoRequest veiculoReq = new VeiculoRequest();
        veiculoReq.setId(1L);
        testRequest.setVeiculo(veiculoReq);

        try {
            kafkaTemplate.send(TOPIC, "test", testRequest)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✅ Mensagem de teste enviada com sucesso");
                        } else {
                            log.error("❌ Falha ao enviar mensagem de teste", ex);
                        }
                    });
            return ResponseEntity.ok("Mensagem de teste enviada para Kafka");
        } catch (Exception e) {
            log.error("❌ Erro ao testar Kafka: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao testar Kafka: " + e.getMessage());
        }
    }

    // =====================================================================
    // DTOs
    // =====================================================================

    public static class TelemetriaRequest {
        private VeiculoRequest veiculo;
        private Double latitude;
        private Double longitude;
        private Double velocidade;
        private Double nivelCombustible;
        private LocalDateTime dataHora;

        public VeiculoRequest getVeiculo() { return veiculo; }
        public void setVeiculo(VeiculoRequest veiculo) { this.veiculo = veiculo; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Double getVelocidade() { return velocidade; }
        public void setVelocidade(Double velocidade) { this.velocidade = velocidade; }
        public Double getNivelCombustible() { return nivelCombustible; }
        public void setNivelCombustible(Double nivelCombustible) { this.nivelCombustible = nivelCombustible; }
        public LocalDateTime getDataHora() { return dataHora; }
        public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

        @Override
        public String toString() {
            return "TelemetriaRequest{veiculoId=" + (veiculo != null ? veiculo.getId() : null)
                    + ", latitude=" + latitude + ", longitude=" + longitude
                    + ", velocidade=" + velocidade + ", dataHora=" + dataHora + '}';
        }
    }

    public static class VeiculoRequest {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        @Override
        public String toString() { return "VeiculoRequest{id=" + id + '}'; }
    }
}