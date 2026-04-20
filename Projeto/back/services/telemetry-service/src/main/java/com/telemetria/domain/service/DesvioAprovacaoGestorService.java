package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.infrastructure.persistence.DesvioRotaRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

/**
 * RN-DEV-002 · Aprovação do Gestor para Desvios de Rota
 * 
 * Regras:
 * - Desvios com aprovado_gestor = NULL há > 24h: reaparecem no painel de pendências
 * - Aprovado: sem impacto no score
 * - Reprovado: dedução retroativa aplicada
 */
@Service
public class DesvioAprovacaoGestorService {

    private static final Logger log = LoggerFactory.getLogger(DesvioAprovacaoGestorService.class);
    
    // Prazo em horas para reaparecer no painel de pendências
    private static final int PRAZO_REAPARECER_HORAS = 24;
    
    // Pontos de penalidade por desvio reprovado
    private static final int PENALIDADE_DESVIO_REPROVADO = 15;
    
    private final DesvioRotaRepository desvioRotaRepository;
    private final ViagemRepository viagemRepository;
    private final AlertaService alertaService;

    public DesvioAprovacaoGestorService(
            DesvioRotaRepository desvioRotaRepository,
            ViagemRepository viagemRepository,
            AlertaService alertaService) {
        this.desvioRotaRepository = desvioRotaRepository;
        this.viagemRepository = viagemRepository;
        this.alertaService = alertaService;
    }

    /**
     * RN-DEV-002: Busca desvios pendentes de aprovação há mais de 24 horas
     * Estes desvios devem reaparecer no painel de pendências
     */
    public List<DesvioRota> buscarDesviosPendentesMais24h() {
        LocalDateTime dataLimite = LocalDateTime.now().minusHours(PRAZO_REAPARECER_HORAS);
        List<DesvioRota> desvios = desvioRotaRepository.findDesviosPendentesMais24h(dataLimite);
        
        if (!desvios.isEmpty()) {
            log.info("📋 RN-DEV-002: {} desvios pendentes há mais de {}h encontrados", 
                    desvios.size(), PRAZO_REAPARECER_HORAS);
        }
        
        return desvios;
    }

    /**
     * RN-DEV-002: Busca todos os desvios aguardando aprovação do gestor
     */
    public List<DesvioRota> buscarDesviosPendentesAprovacao() {
        return desvioRotaRepository.findDesviosPendentesAprovacao();
    }

    /**
     * RN-DEV-002: Aprova um desvio de rota
     * - Sem impacto no score da viagem
     * - Registra gestor e data de aprovação
     */
    @Transactional
    public void aprovarDesvio(Long desvioId, Long gestorId, String justificativa) {
        Optional<DesvioRota> optDesvio = desvioRotaRepository.findById(desvioId);
        
        if (optDesvio.isEmpty()) {
            log.warn("⚠️ Desvio {} não encontrado para aprovação", desvioId);
            throw new IllegalArgumentException("Desvio não encontrado: " + desvioId);
        }
        
        DesvioRota desvio = optDesvio.get();
        
        // Verificar se já foi aprovado/reprovado
        if (desvio.getAprovadoGestor() != null) {
            log.warn("⚠️ Desvio {} já foi avaliado (aprovado={})", desvioId, desvio.getAprovadoGestor());
            throw new IllegalStateException("Desvio já foi avaliado pelo gestor");
        }
        
        // Aprovar desvio
        desvio.setAprovadoGestor(true);
        desvio.setGestorId(gestorId);
        desvio.setDataAprovacaoGestor(LocalDateTime.now());
        desvio.setJustificativaGestor(justificativa);
        
        desvioRotaRepository.save(desvio);
        
        log.info("✅ RN-DEV-002: Desvio {} aprovado pelo gestor {} - Sem impacto no score", 
                desvioId, gestorId);
        
        // Notificar motorista sobre aprovação (opcional)
        notificarAprovacao(desvio, gestorId, justificativa);
    }

    /**
     * RN-DEV-002: Reprova um desvio de rota
     * - Dedução retroativa aplicada ao score da viagem
     * - Registra gestor, data e justificativa
     */
    @Transactional
    public void reprovarDesvio(Long desvioId, Long gestorId, String justificativa) {
        Optional<DesvioRota> optDesvio = desvioRotaRepository.findById(desvioId);
        
        if (optDesvio.isEmpty()) {
            log.warn("⚠️ Desvio {} não encontrado para reprovação", desvioId);
            throw new IllegalArgumentException("Desvio não encontrado: " + desvioId);
        }
        
        DesvioRota desvio = optDesvio.get();
        
        // Verificar se já foi aprovado/reprovado
        if (desvio.getAprovadoGestor() != null) {
            log.warn("⚠️ Desvio {} já foi avaliado (aprovado={})", desvioId, desvio.getAprovadoGestor());
            throw new IllegalStateException("Desvio já foi avaliado pelo gestor");
        }
        
        // Reprovar desvio
        desvio.setAprovadoGestor(false);
        desvio.setGestorId(gestorId);
        desvio.setDataAprovacaoGestor(LocalDateTime.now());
        desvio.setJustificativaGestor(justificativa);
        
        desvioRotaRepository.save(desvio);
        
        log.info("🚫 RN-DEV-002: Desvio {} reprovado pelo gestor {} - Penalidade aplicada", 
                desvioId, gestorId);
        
        // Aplicar dedução retroativa no score
        aplicarPenalidadeScore(desvio);
        
        // Notificar motorista sobre reprovação
        notificarReprovacao(desvio, gestorId, justificativa);
    }

    /**
     * RN-DEV-002: Verifica se há desvios pendentes de aprovação
     * Utilizado para alertar gestores no dashboard
     */
    public boolean existeDesvioPendenteAprovacao() {
        long count = desvioRotaRepository.countByAprovadoGestor(null);
        return count > 0;
    }

    /**
     * RN-DEV-002: Conta desvios pendentes de aprovação
     */
    public long contarDesviosPendentes() {
        return desvioRotaRepository.countByAprovadoGestor(null);
    }

    /**
     * RN-DEV-002: Conta desvios aprovados
     */
    public long contarDesviosAprovados() {
        return desvioRotaRepository.countByAprovadoGestor(true);
    }

    /**
     * RN-DEV-002: Conta desvios reprovados
     */
    public long contarDesviosReprovados() {
        return desvioRotaRepository.countByAprovadoGestor(false);
    }

    /**
     * Aplica penalidade retroativa ao score da viagem quando desvio é reprovado
     */
    private void aplicarPenalidadeScore(DesvioRota desvio) {
        if (desvio.getViagemId() == null) {
            log.warn("⚠️ Desvio {} sem viagem associada - penalidade não aplicada", desvio.getId());
            return;
        }
        
        Optional<Viagem> optViagem = viagemRepository.findById(desvio.getViagemId());
        if (optViagem.isEmpty()) {
            log.warn("⚠️ Viagem {} não encontrada para aplicar penalidade", desvio.getViagemId());
            return;
        }
        
        Viagem viagem = optViagem.get();
        
        // Deduzir pontos do score da viagem
        Integer scoreAtual = viagem.getScoreViagem();
        if (scoreAtual == null) {
            scoreAtual = 1000;
        }
        
        int novoScore = Math.max(0, scoreAtual - PENALIDADE_DESVIO_REPROVADO);
        viagem.setScoreViagem(novoScore);
        
        // Atualizar km fora da rota
        Double kmForaRotaAtual = viagem.getKmForaRota();
        if (kmForaRotaAtual == null) {
            kmForaRotaAtual = 0.0;
        }
        viagem.setKmForaRota(kmForaRotaAtual + (desvio.getKmExtras() != null ? desvio.getKmExtras() : 0.0));
        
        viagemRepository.save(viagem);
        
        log.info("📊 RN-DEV-002: Penalidade aplicada à viagem {} - Score: {} → {} (km extras: {})", 
                viagem.getId(), scoreAtual, novoScore, desvio.getKmExtras());
        
        // Criar alerta sobre penalidade aplicada
        criarAlertaPenalidade(desvio, viagem, scoreAtual, novoScore);
    }

    /**
     * Cria alerta informando sobre a penalidade aplicada
     */
    private void criarAlertaPenalidade(DesvioRota desvio, Viagem viagem, int scoreAnterior, int scoreNovo) {
        try {
            String mensagem = String.format(
                "🚫 DESVIO REPROVADO - Penalide aplicada\n\n" +
                "Desvio ID: %d\n" +
                "Viagem: %d\n" +
                "Score anterior: %d → Novo score: %d (-%d pontos)\n" +
                "Km extras do desvio: %.2f km\n\n" +
                "O desvio foi reprovado pelo gestor e a dedução foi aplicada retroativamente.",
                desvio.getId(),
                viagem.getId(),
                scoreAnterior,
                scoreNovo,
                PENALIDADE_DESVIO_REPROVADO,
                desvio.getKmExtras() != null ? desvio.getKmExtras() : 0.0
            );
            
            alertaService.criarAlertaCompleto(
                desvio.getTenantId(),
                desvio.getVeiculoId(),
                desvio.getVeiculoUuid(),
                viagem.getId(),
                com.telemetria.domain.enums.TipoAlerta.DESVIO_ROTA_REPROVADO,
                com.telemetria.domain.enums.SeveridadeAlerta.ALTO,
                mensagem,
                desvio.getLatitudeDesvio(),
                desvio.getLongitudeDesvio(),
                desvio.getVelocidadeKmh(),
                null
            );
            
            log.info("📢 Alerta de penalidade criado para viagem {}", viagem.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao criar alerta de penalidade: {}", e.getMessage());
        }
    }

    /**
     * Notifica sobre aprovação do desvio
     */
    private void notificarAprovacao(DesvioRota desvio, Long gestorId, String justificativa) {
        log.info("📧 Notificação de aprovação enviada - Desvio: {}, Gestor: {}", 
                desvio.getId(), gestorId);
        // TODO: Implementar notificação real (email, push, etc.)
    }

    /**
     * Notifica sobre reprovação do desvio
     */
    private void notificarReprovacao(DesvioRota desvio, Long gestorId, String justificativa) {
        log.info("📧 Notificação de reprovação enviada - Desvio: {}, Gestor: {}, Justificativa: {}", 
                desvio.getId(), gestorId, justificativa);
        // TODO: Implementar notificação real (email, push, etc.)
    }

    /**
     * Calcula há quantas horas o desvio está pendente
     */
    public long calcularHorasPendentes(DesvioRota desvio) {
        if (desvio.getCriadoEm() == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(desvio.getCriadoEm(), LocalDateTime.now());
    }

    /**
     * Verifica se o desvio está pendente há mais de 24 horas
     */
    public boolean isPendenteMais24h(DesvioRota desvio) {
        return calcularHorasPendentes(desvio) > PRAZO_REAPARECER_HORAS;
    }
}
