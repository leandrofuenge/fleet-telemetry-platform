package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaSatelitesInsuficientesGeradoEvent;

@Component
public class AlertaSatelitesInsuficientesEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaSatelitesInsuficientesEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public AlertaSatelitesInsuficientesEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background, liberando o fluxo de processamento de GPS imediatamente
    @EventListener
    public void handleSatelitesInsuficientesGerado(AlertaSatelitesInsuficientesGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de satélites insuficientes ID: {}", event.getAlerta().getId());
            messagingTemplate.convertAndSend("/topic/alertas/satelites-insuficientes", event.getAlerta());
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de satélites insuficientes via WebSocket: {}", e.getMessage());
        }
    }
}