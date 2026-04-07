// src/main/java/com/telemetria/application/scheduler/ParadaMonitorScheduler.java
package com.telemetria.application.scheduler;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.entity.HistoricoETA;
import com.telemetria.domain.entity.PosicaoAtual;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.StatusViagem;
import com.telemetria.domain.service.ETACalculationService;
import com.telemetria.infrastructure.persistence.HistoricoETARepository;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Component
public class ParadaMonitorScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ParadaMonitorScheduler.class);
    
    @Autowired
    private ViagemRepository viagemRepository;
    
    @Autowired
    private PosicaoAtualRepository posicaoAtualRepository;
    
    @Autowired
    private HistoricoETARepository historicoETARepository;
    
    @Autowired
    private ETACalculationService etaCalculationService;
    
    @Value("${eta.tempo.parada.para.recalculo:10}")
    private int tempoParadaParaRecalculo;
    
    @Value("${eta.tempo.parada.indeterminado:30}")
    private int tempoParadaIndeterminado;
    
    /**
     * Monitora paradas prolongadas a cada 1 minuto
     * - Parada > 10 minutos: dispara recálculo de ETA
     * - Parada > 30 minutos sem previsão: marca ETA como INDETERMINADO
     */
    @Scheduled(fixedDelay = 60000) // 1 minuto
    public void monitorarParadas() {
        log.debug("⏸️ [PARADA MONITOR] Verificando paradas prolongadas");
        
        List<Viagem> viagensAtivas = viagemRepository.findByStatus(StatusViagem.EM_ANDAMENTO.name());
        
        for (Viagem viagem : viagensAtivas) {
            try {
                Optional<PosicaoAtual> optPosicao = posicaoAtualRepository.findById(viagem.getVeiculoId());
                if (optPosicao.isEmpty()) continue;
                
                PosicaoAtual posicao = optPosicao.get();
                
                // Verificar se está parado
                if (posicao.getVelocidade() != null && posicao.getVelocidade() == 0) {
                    int tempoParado = calcularTempoParado(viagem.getVeiculoId());
                    
                    // RN-VIA-003: Parada > 10 minutos - recalcular ETA
                    if (tempoParado >= tempoParadaParaRecalculo && tempoParado < tempoParadaIndeterminado) {
                        log.info("⏸️ [PARADA MONITOR] Veículo {} parado por {} minutos - Recalculando ETA", 
                                 viagem.getVeiculoId(), tempoParado);
                        etaCalculationService.recalcularETA(viagem.getId(), "PARADA_10_MINUTOS");
                    }
                    
                    // Parada > 30 minutos sem previsão - ETA INDETERMINADO
                    else if (tempoParado >= tempoParadaIndeterminado) {
                        Optional<HistoricoETA> ultimoETA = historicoETARepository
                            .findTopByViagemIdOrderByDataCalculoDesc(viagem.getId());
                        
                        if (ultimoETA.isPresent() && !"INDETERMINADO".equals(ultimoETA.get().getStatusEta())) {
                            log.warn("⏸️🚨 [PARADA MONITOR] Veículo {} parado por {} minutos sem previsão - ETA INDETERMINADO", 
                                     viagem.getVeiculoId(), tempoParado);
                            etaCalculationService.recalcularETA(viagem.getId(), "PARADA_30_MINUTOS_SEM_PREVISAO");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao monitorar parada para viagem {}: {}", viagem.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Calcula há quanto tempo o veículo está parado
     */
    private int calcularTempoParado(Long veiculoId) {
        // Buscar últimas telemetrias para calcular tempo de parada
        // Implementação simplificada - ajustar conforme necessidade
        Optional<HistoricoETA> ultimoETA = historicoETARepository
            .findTopByVeiculoIdOrderByDataCalculoDesc(veiculoId);
        
        if (ultimoETA.isPresent() && ultimoETA.get().getTempoParadoMinutos() != null) {
            return ultimoETA.get().getTempoParadoMinutos();
        }
        
        return 0;
    }
}