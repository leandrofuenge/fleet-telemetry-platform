package com.app.telemetria.service;

import com.app.telemetria.entity.Telemetria;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.repository.TelemetriaRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class TelemetriaService {
    
    private final TelemetriaRepository telemetriaRepository;
    
    public TelemetriaService(TelemetriaRepository telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
    }
    
    public Optional<Telemetria> buscarUltimaPorVeiculo(Long veiculoId) {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(veiculoId);
        return telemetriaRepository.findUltimaTelemetriaByVeiculo(veiculo);
    }
}