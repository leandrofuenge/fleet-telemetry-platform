package com.telemetria.application.scheduler;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@Component
public class TelemetriaRetentionScheduler {
    
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private TelemetriaRepository telemetriaRepository;

    @Scheduled(cron = "0 0 2 1 * ?") // 2h do dia 1
    @Transactional
    public void limparTelemetriaAntiga() {
        System.out.println("🧹 [RF06] Iniciando limpeza mensal telemetria");
        
        Map<String, Integer> retencaoPorPlano = Map.of(
            "STARTER", 60, "PRO", 180, "ENTERPRISE", 365
        );
        
        veiculoRepository.findAll().forEach(veiculo -> {
            if (!veiculo.getAtivo()) return;
            
            int diasRetencao = retencaoPorPlano.getOrDefault(veiculo.getPlano(), 60);
            
            // ✅ RN-POS-002: RESPEITAR preservar_dados + jornada
            int deletados = telemetriaRepository.deleteByVeiculoIdAndDataHoraBeforeAndPreservarDadosFalse(
                veiculo.getId(), LocalDateTime.now().minusDays(diasRetencao));
                
            System.out.println("🗑️ [RF06] Veículo " + veiculo.getPlaca() + " (" + veiculo.getPlano() + "): " 
                + deletados + " registros deletados (" + diasRetencao + " dias)");
        });
        
        System.out.println("🧹 [RF06] Limpeza mensal CONCLUÍDA");
    }
}