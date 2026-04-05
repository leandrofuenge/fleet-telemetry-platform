package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class TelemetriaService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaService.class);

    private final TelemetriaRepository telemetriaRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;

    @Autowired
    public TelemetriaService(TelemetriaRepository telemetriaRepository, 
                           PosicaoAtualRepository posicaoAtualRepository) {
        this.telemetriaRepository = telemetriaRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
    }

    /**
     * Salva telemetria no banco de dados com logs de debug
     */
    public Telemetria salvar(Telemetria telemetria) {
        log.info("💾 [SERVICE] Iniciando salvamento da telemetria");
        log.debug("[SERVICE] Veículo ID: {}, Data/Hora: {}, Lat: {}, Lng: {}",
                telemetria.getVeiculoId(),
                telemetria.getDataHora(),
                telemetria.getLatitude(),
                telemetria.getLongitude());
        
        if (telemetria.getVeiculoId() == null) {
            log.error("❌ [SERVICE] ERRO: veiculoId é nulo!");
            throw new IllegalArgumentException("veiculoId não pode ser nulo");
        }
        if (telemetria.getTenantId() == null) {
            log.error("❌ [SERVICE] ERRO: tenantId é nulo!");
            throw new IllegalArgumentException("tenantId não pode ser nulo");
        }
        
        Telemetria saved = telemetriaRepository.save(telemetria);
        log.info("✅ [SERVICE] Telemetria salva com sucesso! ID: {}", saved.getId());
        
        return saved;
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