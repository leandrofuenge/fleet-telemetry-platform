package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository; // ✅ NOVO RF06
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class TelemetriaService {

    private final TelemetriaRepository telemetriaRepository;
    private final PosicaoAtualRepository posicaoAtualRepository; // ✅ RF06 NOVO

    @Autowired
    public TelemetriaService(TelemetriaRepository telemetriaRepository, 
                           PosicaoAtualRepository posicaoAtualRepository) {
        this.telemetriaRepository = telemetriaRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
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

    // ✅ RF06 RN-POS-001: UPSERT Posição Atual
    public void atualizarPosicaoAtual(Long veiculoId, Long tenantId, String veiculoUuid,
            Double latitude, Double longitude, Double velocidade, Double direcao,
            Boolean ignicao, LocalDateTime ultimaTelemetria) {
        System.out.println("📍 [RF06] UPSERT posição atual - Veículo: " + veiculoId);
        posicaoAtualRepository.upsertPosicaoAtual(veiculoId, tenantId, veiculoUuid,
                latitude, longitude, velocidade, direcao, ignicao, "ONLINE", ultimaTelemetria);
        System.out.println("✅ [RF06] Posição atualizada com sucesso");
    }

    // ✅ RF06 - Veículos sem sinal (30min + ignição)
    public List<Long> findVeiculosSemSinal(int minutosSemSinal, Boolean ultimaIgnicaoOn) {
        return telemetriaRepository.findVeiculosSemSinal(minutosSemSinal, ultimaIgnicaoOn);
    }
}