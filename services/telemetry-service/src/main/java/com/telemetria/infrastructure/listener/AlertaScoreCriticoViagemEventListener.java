package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaScoreCriticoViagemGeradoEvent;

@Component
public class AlertaScoreCriticoViagemEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaScoreCriticoViagemEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    // Construtor manual seguro contra falhas de compilação da IDE
    public AlertaScoreCriticoViagemEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em uma Thread pool separada. Não trava o fluxo de processamento de viagens!
    @EventListener
    public void handleScoreCriticoViagemGerado(AlertaScoreCriticoViagemGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de score crítico de viagem ID: {}", event.getAlerta().getViagemId());
            
            // Tópico correspondente para o painel de viagens no seu front-end
            messagingTemplate.convertAndSend("/topic/alertas/viagem-score-critico", event.getAlerta());
            
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de score crítico de viagem via WebSocket: {}", e.getMessage());
        }
    }
}