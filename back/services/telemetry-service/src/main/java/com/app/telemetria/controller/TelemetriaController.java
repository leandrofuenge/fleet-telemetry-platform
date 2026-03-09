package com.app.telemetria.controller;

import com.app.telemetria.entity.Telemetria;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.repository.TelemetriaRepository;
import com.app.telemetria.repository.VeiculoRepository;
import com.app.telemetria.repository.ViagemRepository;
import com.app.telemetria.service.AlertaService;
import com.app.telemetria.service.WeatherAlertService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/telemetria")
public class TelemetriaController {
    
    private final TelemetriaRepository telemetriaRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemRepository viagemRepository;
    private final AlertaService alertaService;
    private final WeatherAlertService weatherAlertService;
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
        this.viagemRepository = viagemRepository;
        this.alertaService = alertaService;
        this.weatherAlertService = weatherAlertService;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @PostMapping
    public ResponseEntity<String> criar(@RequestBody TelemetriaRequest request) {
     

        Veiculo veiculo = veiculoRepository.findById(request.getVeiculo().getId())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.VEICULO_NOT_FOUND,
                request.getVeiculo().getId().toString()
            ));
        
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                "Latitude e longitude são obrigatórios");
        }
        
        // Envia para processamento em background
        CompletableFuture.supplyAsync(() -> {
            kafkaTemplate.send(TOPIC, veiculo.getId().toString(), request);
            return "Mensagem enviada para processamento";
        });
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Telemetria recebida e em processamento assíncrono. ID do veículo: " + veiculo.getId());
    }
    
    @GetMapping("/veiculo/{veiculoId}")
    public List<Telemetria> listarPorVeiculo(@PathVariable Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.VEICULO_NOT_FOUND,
                veiculoId.toString()
            ));
            
        return telemetriaRepository.findByVeiculoOrderByDataHoraDesc(veiculo);
    }
    
    @GetMapping("/veiculo/{veiculoId}/ultima")
    public ResponseEntity<Telemetria> ultimaTelemetria(@PathVariable Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.VEICULO_NOT_FOUND,
                veiculoId.toString()
            ));
        
        return telemetriaRepository.findUltimaTelemetriaByVeiculo(veiculo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/veiculo/{veiculoId}/periodo")
    public List<Telemetria> listarPorPeriodo(
            @PathVariable Long veiculoId,
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) {
        
        if (inicio.isAfter(fim)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                "Data de início não pode ser posterior à data de fim");
        }
        
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.VEICULO_NOT_FOUND,
                veiculoId.toString()
            ));
        
        return telemetriaRepository.findByVeiculoAndDataHoraBetweenOrderByDataHoraAsc(
            veiculo, inicio, fim);
    }
    
    // DTO para receber os dados
    public static class TelemetriaRequest {
        private VeiculoRequest veiculo;
        private Double latitude;
        private Double longitude;
        private Double velocidade;
        private Double nivelCombustible;  
        private LocalDateTime dataHora;
        
        // Getters e Setters
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
    }
    
    public static class VeiculoRequest {
        private Long id;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}