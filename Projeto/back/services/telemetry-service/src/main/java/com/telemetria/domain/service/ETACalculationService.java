// src/main/java/com/telemetria/domain/service/ETACalculationService.java
package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.api.dto.response.ETAResponseDTO;
import com.telemetria.api.dto.response.RouteResponse;
import com.telemetria.domain.entity.HistoricoETA;
import com.telemetria.domain.entity.PosicaoAtual;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.StatusViagem;
import com.telemetria.infrastructure.integration.routing.RoutingClient;
import com.telemetria.infrastructure.persistence.HistoricoETARepository;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository;
import com.telemetria.infrastructure.persistence.RotaRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class ETACalculationService {
    
    private static final Logger log = LoggerFactory.getLogger(ETACalculationService.class);
    
    @Autowired
    private ViagemRepository viagemRepository;
    
    @Autowired
    private RotaRepository rotaRepository;
    
    @Autowired
    private PosicaoAtualRepository posicaoAtualRepository;
    
    @Autowired
    private HistoricoETARepository historicoETARepository;
    
    @Autowired
    private RoutingClient routingClient;
    
    @Autowired
    private AlertaService alertaService;
    
    @Value("${eta.limite.atraso.leve:15}")
    private int limiteAtrasoLeve;
    
    @Value("${eta.limite.atraso.moderado:30}")
    private int limiteAtrasoModerado;
    
    @Value("${eta.limite.atraso.critico:60}")
    private int limiteAtrasoCritico;
    
    @Value("${eta.tempo.parada.para.recalculo:10}")
    private int tempoParadaParaRecalculo;
    
    @Value("${eta.tempo.parada.indeterminado:30}")
    private int tempoParadaIndeterminado;
    
    @Transactional
    public Optional<HistoricoETA> recalcularETA(Long viagemId, String motivo) {
        log.info("📊 Recalculando ETA para viagem {} - Motivo: {}", viagemId, motivo);
        
        Optional<Viagem> optViagem = viagemRepository.findById(viagemId);
        if (optViagem.isEmpty()) {
            log.warn("Viagem {} não encontrada", viagemId);
            return Optional.empty();
        }
        
        Viagem viagem = optViagem.get();
        
        if (!"EM_ANDAMENTO".equals(viagem.getStatus())) {
            log.debug("Viagem {} não está em andamento. Status: {}", viagemId, viagem.getStatus());
            return Optional.empty();
        }
        
        Optional<PosicaoAtual> optPosicao = posicaoAtualRepository.findById(viagem.getVeiculoId());
        if (optPosicao.isEmpty()) {
            log.warn("Posição atual não encontrada para veículo {}", viagem.getVeiculoId());
            return Optional.empty();
        }
        
        PosicaoAtual posicao = optPosicao.get();
        
        Optional<Rota> optRota = rotaRepository.findById(viagem.getRotaId());
        if (optRota.isEmpty()) {
            log.warn("Rota {} não encontrada", viagem.getRotaId());
            return Optional.empty();
        }
        
        Rota rota = optRota.get();
        
        try {
            RouteResponse routeResponse = routingClient.calcular(
                posicao.getLatitude(),
                posicao.getLongitude(),
                rota.getLatitudeDestino(),
                rota.getLongitudeDestino()
            );
            
            if (routeResponse == null) {
                log.error("Falha ao calcular rota - OSRM retornou null");
                return Optional.empty();
            }
            
            double minutosRestantes = routeResponse.getDuracaoMinutos();
            LocalDateTime etaCalculado = LocalDateTime.now().plusMinutes((long) minutosRestantes);
            LocalDateTime etaPrevistoOriginal = viagem.getDataChegadaPrevista();
            
            boolean paradaPrevista = verificarParadaPrevista(posicao, viagem);
            Integer tempoParadoMinutos = calcularTempoParado(posicao);
            
            String statusEta = "NORMAL";
            String motivoIndeterminado = null;
            
            if (!paradaPrevista && tempoParadoMinutos >= tempoParadaIndeterminado && posicao.getVelocidade() == 0) {
                statusEta = "INDETERMINADO";
                motivoIndeterminado = String.format(
                    "Veículo parado por %d minutos sem ponto de parada previsto na rota",
                    tempoParadoMinutos
                );
                minutosRestantes = -1;
                log.warn("⏰ ETA INDETERMINADO para viagem {} - {}", viagemId, motivoIndeterminado);
            }
            
            Long atrasoMinutos = null;
            if (etaPrevistoOriginal != null && etaCalculado != null) {
                atrasoMinutos = java.time.Duration.between(etaPrevistoOriginal, etaCalculado).toMinutes();
                if (atrasoMinutos > 0) {
                    statusEta = classificarAtraso(atrasoMinutos);
                    log.info("⏰ Atraso detectado: {} minutos - Classificação: {}", atrasoMinutos, statusEta);
                }
            }
            
            Optional<HistoricoETA> ultimoHistorico = historicoETARepository.findTopByViagemIdOrderByDataCalculoDesc(viagemId);
            boolean notificacao30minEnviada = ultimoHistorico.map(h -> h.getNotificacaoEnviada30min()).orElse(false);
            boolean notificacao60minEnviada = ultimoHistorico.map(h -> h.getNotificacaoEnviada60min()).orElse(false);
            
            if (atrasoMinutos != null && atrasoMinutos >= limiteAtrasoModerado) {
                if (atrasoMinutos >= limiteAtrasoCritico && !notificacao60minEnviada) {
                    enviarNotificacaoAtrasoCritico(viagem, posicao, atrasoMinutos);
                    notificacao60minEnviada = true;
                } else if (atrasoMinutos >= limiteAtrasoModerado && !notificacao30minEnviada) {
                    enviarNotificacaoAtrasoModerado(viagem, posicao, atrasoMinutos);
                    notificacao30minEnviada = true;
                }
            }
            
            HistoricoETA historico = HistoricoETA.builder()
                .viagemId(viagemId)
                .veiculoId(viagem.getVeiculoId())
                .latitudeAtual(posicao.getLatitude())
                .longitudeAtual(posicao.getLongitude())
                .minutosRestantes(minutosRestantes == -1 ? null : minutosRestantes)
                .distanciaRestanteKm(routeResponse.getDistanciaKm())
                .etaPrevisto(etaPrevistoOriginal)
                .etaCalculado(etaCalculado)
                .atrasoMinutos(atrasoMinutos != null && atrasoMinutos > 0 ? atrasoMinutos : 0)
                .statusEta(statusEta)
                .motivoIndeterminado(motivoIndeterminado)
                .velocidadeAtualKmh(posicao.getVelocidade())
                .tempoParadoMinutos(tempoParadoMinutos)
                .paradaPrevista(paradaPrevista)
                .dataCalculo(LocalDateTime.now())
                .notificacaoEnviada30min(notificacao30minEnviada)
                .notificacaoEnviada60min(notificacao60minEnviada)
                .build();
            
            HistoricoETA salvo = historicoETARepository.save(historico);
            log.info("✅ ETA calculado para viagem {}: {} min restantes, Status: {}", 
                     viagemId, minutosRestantes, statusEta);
            
            return Optional.of(salvo);
            
        } catch (Exception e) {
            log.error("Erro ao recalcular ETA para viagem {}: {}", viagemId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    private boolean verificarParadaPrevista(PosicaoAtual posicao, Viagem viagem) {
        return false;
    }
    
    private Integer calcularTempoParado(PosicaoAtual posicao) {
        if (posicao.getVelocidade() != null && posicao.getVelocidade() > 0) {
            return 0;
        }
        return 0;
    }
    
    private String classificarAtraso(long atrasoMinutos) {
        if (atrasoMinutos >= limiteAtrasoCritico) {
            return "ATRASO_CRITICO";
        } else if (atrasoMinutos >= limiteAtrasoModerado) {
            return "ATRASO_MODERADO";
        } else if (atrasoMinutos >= limiteAtrasoLeve) {
            return "ATRASO_LEVE";
        }
        return "NORMAL";
    }
    
    private void enviarNotificacaoAtrasoModerado(Viagem viagem, PosicaoAtual posicao, long atrasoMinutos) {
        log.info("📧 Enviando notificação de ATRASO MODERADO ({} min) para viagem {}", atrasoMinutos, viagem.getId());
        
        String mensagem = String.format(
            "🚛 ATRASO DE %d MINUTOS\n\nViagem: %s\nVeículo: %s\nMotorista: %s\n" +
            "Localização: %.6f, %.6f\nAtraso: %d min\nPrevisão: %s",
            atrasoMinutos, viagem.getId(), viagem.getVeiculoId(), viagem.getMotoristaId(),
            posicao.getLatitude(), posicao.getLongitude(), atrasoMinutos,
            LocalDateTime.now().plusMinutes(calcularMinutosRestantes(viagem))
        );
        
        String veiculoUuid = viagem.getVeiculo() != null ? viagem.getVeiculo().getUuid() : null;
        alertaService.criarAlertaCompleto(
            viagem.getTenantId(),
            viagem.getVeiculoId(),
            veiculoUuid,
            viagem.getId(),
            com.telemetria.domain.enums.TipoAlerta.ATRASO_VIAGEM,
            com.telemetria.domain.enums.SeveridadeAlerta.ALTO,
            mensagem,
            posicao.getLatitude(),
            posicao.getLongitude(),
            posicao.getVelocidade(),
            null
        );
    }
    
    private void enviarNotificacaoAtrasoCritico(Viagem viagem, PosicaoAtual posicao, long atrasoMinutos) {
        log.error("📧🚨 ATRASO CRÍTICO ({} min) para viagem {}", atrasoMinutos, viagem.getId());
        
        String mensagem = String.format(
            "🚨 ATRASO CRÍTICO DE %d MINUTOS\n\nViagem: %s\nVeículo: %s\nMotorista: %s\n" +
            "Localização: %.6f, %.6f\nAtraso: %d min\nPrevisão: %s",
            atrasoMinutos, viagem.getId(), viagem.getVeiculoId(), viagem.getMotoristaId(),
            posicao.getLatitude(), posicao.getLongitude(), atrasoMinutos,
            LocalDateTime.now().plusMinutes(calcularMinutosRestantes(viagem))
        );
        
        String veiculoUuidCritico = viagem.getVeiculo() != null ? viagem.getVeiculo().getUuid() : null;
        alertaService.criarAlertaCompleto(
            viagem.getTenantId(),
            viagem.getVeiculoId(),
            veiculoUuidCritico,
            viagem.getId(),
            com.telemetria.domain.enums.TipoAlerta.ATRASO_VIAGEM,
            com.telemetria.domain.enums.SeveridadeAlerta.CRITICO,
            mensagem,
            posicao.getLatitude(),
            posicao.getLongitude(),
            posicao.getVelocidade(),
            null
        );
    }
    
    private long calcularMinutosRestantes(Viagem viagem) {
        Optional<HistoricoETA> ultimoETA = historicoETARepository.findTopByViagemIdOrderByDataCalculoDesc(viagem.getId());
        if (ultimoETA.isPresent() && ultimoETA.get().getMinutosRestantes() != null) {
            return ultimoETA.get().getMinutosRestantes().longValue();
        }
        return 60;
    }
    
    @Transactional
    public int recalcularETAEmLote() {
        log.info("📊 Recalculando ETA para todas as viagens em andamento");
        
        List<Viagem> viagensAtivas = viagemRepository.findByStatus(StatusViagem.EM_ANDAMENTO.name());
        int sucessos = 0;
        
        for (Viagem viagem : viagensAtivas) {
            Optional<HistoricoETA> resultado = recalcularETA(viagem.getId(), "RECALCULO_PERIODICO");
            if (resultado.isPresent()) {
                sucessos++;
            }
        }
        
        log.info("✅ ETA recalculado para {}/{} viagens", sucessos, viagensAtivas.size());
        return sucessos;
    }
    
    public ETAResponseDTO gerarResponseDTO(Viagem viagem) {
        Optional<HistoricoETA> ultimoETA = historicoETARepository.findTopByViagemIdOrderByDataCalculoDesc(viagem.getId());
        Optional<PosicaoAtual> posicao = posicaoAtualRepository.findById(viagem.getVeiculoId());
        
        if (ultimoETA.isEmpty() || posicao.isEmpty()) {
            return null;
        }
        
        HistoricoETA eta = ultimoETA.get();
        PosicaoAtual pos = posicao.get();
        
        String mensagemStatus;
        switch (eta.getStatusEta()) {
            case "ATRASO_CRITICO":
                mensagemStatus = "⚠️🚨 ATRASO CRÍTICO";
                break;
            case "ATRASO_MODERADO":
                mensagemStatus = "⚠️ ATRASO SIGNIFICATIVO";
                break;
            case "ATRASO_LEVE":
                mensagemStatus = "⏰ Pequeno atraso";
                break;
            case "INDETERMINADO":
                mensagemStatus = "❓ ETA INDETERMINADO";
                break;
            default:
                mensagemStatus = "✅ No prazo";
        }
        
        return ETAResponseDTO.builder()
            .viagemId(viagem.getId())
            .veiculoId(viagem.getVeiculoId())
            .latitudeAtual(pos.getLatitude())
            .longitudeAtual(pos.getLongitude())
            .velocidadeAtualKmh(pos.getVelocidade())
            .distanciaRestanteKm(eta.getDistanciaRestanteKm())
            .minutosRestantes(eta.getMinutosRestantes())
            .etaCalculado(eta.getEtaCalculado())
            .etaPrevistoOriginal(eta.getEtaPrevisto())
            .atrasoMinutos(eta.getAtrasoMinutos())
            .statusEta(eta.getStatusEta())
            .mensagemStatus(mensagemStatus)
            .paradaNaoPrevistaDetectada(!eta.getParadaPrevista() && eta.getTempoParadoMinutos() >= tempoParadaIndeterminado)
            .tempoParadoMinutos(eta.getTempoParadoMinutos())
            .ultimaAtualizacao(eta.getDataCalculo())
            .build();
    }
}
