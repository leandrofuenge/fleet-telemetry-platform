// =====================================================================
// TelemetriaService.java
// =====================================================================
package com.app.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.app.telemetria.domain.entity.Telemetria;
import com.app.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class TelemetriaService {

    private final TelemetriaRepository telemetriaRepository;

    public TelemetriaService(TelemetriaRepository telemetriaRepository) {
        this.telemetriaRepository = telemetriaRepository;
    }

    public Optional<Telemetria> buscarUltimaPorVeiculo(Long veiculoId) {
        return telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId);
    }

    public List<Telemetria> listarPorVeiculo(Long veiculoId) {
        return telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId);
    }

    public List<Telemetria> listarPorPeriodo(Long veiculoId, LocalDateTime inicio, LocalDateTime fim) {
        return telemetriaRepository.findByVeiculoIdAndDataHoraBetween(veiculoId, inicio, fim);
    }
}
