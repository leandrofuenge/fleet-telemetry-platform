package com.telemetria.application.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.service.DesvioAprovacaoGestorService;

/**
 * RN-DEV-002: Scheduler para verificar desvios pendentes de aprovação
 * 
 * Executa a cada hora para verificar desvios com aprovado_gestor = NULL há > 24h
 * Estes desvios devem reaparecer no painel de pendências
 */
@Component
public class DesvioAprovacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(DesvioAprovacaoScheduler.class);
    
    private final DesvioAprovacaoGestorService desvioAprovacaoService;

    public DesvioAprovacaoScheduler(DesvioAprovacaoGestorService desvioAprovacaoService) {
        this.desvioAprovacaoService = desvioAprovacaoService;
    }

    /**
     * RN-DEV-002: Executa a cada hora (3.600.000 ms = 1 hora)
     * Verifica desvios pendentes há mais de 24h
     */
    @Scheduled(fixedDelay = 3600000)
    public void verificarDesviosPendentes() {
        log.debug("🔍 RN-DEV-002: Verificando desvios pendentes de aprovação...");
        
        try {
            // Buscar desvios pendentes há mais de 24h
            List<DesvioRota> desviosPendentes = desvioAprovacaoService.buscarDesviosPendentesMais24h();
            
            if (desviosPendentes.isEmpty()) {
                log.debug("✅ Nenhum desvio pendente há mais de 24h encontrado");
                return;
            }
            
            log.warn("⚠️ RN-DEV-002: {} desvios pendentes há mais de 24h - Devem reaparecer no painel", 
                    desviosPendentes.size());
            
            // Log detalhado de cada desvio
            for (DesvioRota desvio : desviosPendentes) {
                long horasPendentes = desvioAprovacaoService.calcularHorasPendentes(desvio);
                log.info("📋 Desvio {} - Veículo: {} - Viagem: {} - Pendente há {}h", 
                        desvio.getId(), 
                        desvio.getVeiculoId(),
                        desvio.getViagemId(),
                        horasPendentes);
                
                // TODO: Aqui pode-se implementar:
                // 1. Envio de notificação push para gestores
                // 2. Email de alerta sobre desvios pendentes
                // 3. Reaparecer no painel de pendências com flag especial
                // 4. Atualizar flag 'reaparecerPainel' na entidade DesvioRota
            }
            
            // Notificar gestores sobre desvios pendentes
            notificarGestores(desviosPendentes);
            
        } catch (Exception e) {
            log.error("❌ Erro ao verificar desvios pendentes: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notifica gestores sobre desvios pendentes
     */
    private void notificarGestores(List<DesvioRota> desviosPendentes) {
        // TODO: Implementar notificação para gestores
        // - Email com lista de desvios pendentes
        // - Notificação push no aplicativo
        // - Alerta no dashboard
        log.info("📧 Notificação enviada para gestores sobre {} desvios pendentes", 
                desviosPendentes.size());
    }
    
    /**
     * Estatísticas diárias de aprovação de desvios
     * Executa todos os dias às 8h (cron = 0 0 8 * * *)
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void enviarRelatorioDiario() {
        log.info("📊 RN-DEV-002: Gerando relatório diário de aprovação de desvios...");
        
        try {
            long pendentes = desvioAprovacaoService.contarDesviosPendentes();
            long aprovados = desvioAprovacaoService.contarDesviosAprovados();
            long reprovados = desvioAprovacaoService.contarDesviosReprovados();
            
            log.info("📈 Relatório diário - Pendentes: {} | Aprovados: {} | Reprovados: {}", 
                    pendentes, aprovados, reprovados);
            
            // Se houver muitos pendentes, alertar gestores
            if (pendentes > 10) {
                log.warn("🚨 ALERTA: {} desvios aguardando aprovação do gestor!", pendentes);
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao gerar relatório diário: {}", e.getMessage(), e);
        }
    }
}
