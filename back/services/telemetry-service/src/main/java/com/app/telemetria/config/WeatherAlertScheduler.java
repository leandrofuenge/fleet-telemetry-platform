package com.app.telemetria.config;

import com.app.telemetria.service.VeiculoService;
import com.app.telemetria.service.WeatherAlertService;
import com.app.telemetria.service.ViagemService;
import com.app.telemetria.service.TelemetriaService; // Você precisará criar este service
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class WeatherAlertScheduler {
    
    private final VeiculoService veiculoService;
    private final ViagemService viagemService;
    private final WeatherAlertService weatherAlertService;
    private final TelemetriaService telemetriaService; // NOVO: Para buscar localização
    
    public WeatherAlertScheduler(
            VeiculoService veiculoService,
            ViagemService viagemService,
            WeatherAlertService weatherAlertService,
            TelemetriaService telemetriaService) {
        this.veiculoService = veiculoService;
        this.viagemService = viagemService;
        this.weatherAlertService = weatherAlertService;
        this.telemetriaService = telemetriaService;
    }
    
    @Scheduled(fixedDelay = 900000) // A cada 15 minutos
    public void verificarClimaParaTodosVeiculos() {
        veiculoService.listarTodos().forEach(veiculoDTO -> {
            // Buscar última telemetria do veículo para obter localização
            telemetriaService.buscarUltimaPorVeiculo(veiculoDTO.getId()).ifPresent(ultimaTelemetria -> {
                
                // Buscar viagem ativa do veículo
                viagemService.listarEmAndamento().stream()
                    .filter(v -> v.getVeiculo().getId().equals(veiculoDTO.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                        viagem -> {

                            weatherAlertService.verificarClimaParaVeiculo(
                                veiculoDTO.getId(),
                                ultimaTelemetria.getLatitude(),
                                ultimaTelemetria.getLongitude(),
                                viagem
                            );
                        },
                        () -> {

                            weatherAlertService.verificarClimaParaVeiculo(
                                veiculoDTO.getId(),
                                ultimaTelemetria.getLatitude(),
                                ultimaTelemetria.getLongitude(),
                                null
                            );
                        }
                    );
            });
        });
    }
}