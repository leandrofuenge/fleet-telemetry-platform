package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaGenericoGeradoEvent;

@Component
public class AlertaGenericoEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaGenericoEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public AlertaGenericoEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background isolado
    @EventListener
    public void handleAlertaGenericoGerado(AlertaGenericoGeradoEvent event) {
        try {
            // Roteia de forma inteligente dinâmica para o tópico geral de monitoramento da frota
            String topico = "/topic/alertas/geral";
            log.debug("📡 [WebSocket Despache] Roteando alerta genérico ID: {} para o tópico: {}", event.getAlerta().getId(), topico);
            
            messagingTemplate.convertAndSend(topico, event.getAlerta());
        } catch (Exception e) {
            log.error("❌ Falha ao despachar alerta genérico via WebSocket: {}", e.getMessage());
        }
    }
}