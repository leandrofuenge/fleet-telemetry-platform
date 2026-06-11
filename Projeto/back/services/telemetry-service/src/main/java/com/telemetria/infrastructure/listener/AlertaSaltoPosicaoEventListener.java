package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaSaltoPosicaoGeradoEvent;

@Component
public class AlertaSaltoPosicaoEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaSaltoPosicaoEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public AlertaSaltoPosicaoEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background, sem prender o fluxo principal de processamento de posições
    @EventListener
    public void handleSaltoPosicaoGerado(AlertaSaltoPosicaoGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de salto de posição ID: {}", event.getAlerta().getId());
            
            // Tópico configurado para escuta no seu front-end
            messagingTemplate.convertAndSend("/topic/alertas/salto-posicao", event.getAlerta());
            
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de salto de posição via WebSocket: {}", e.getMessage());
        }
    }
}