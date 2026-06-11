package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaHdopAltoGeradoEvent;

@Component
public class AlertaHdopAltoEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaHdopAltoEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public AlertaHdopAltoEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background liberando a gravação de telemetria instantaneamente
    @EventListener
    public void handleHdopAltoGerado(AlertaHdopAltoGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de HDOP Alto ID: {}", event.getAlerta().getId());
            messagingTemplate.convertAndSend("/topic/alertas/hdop-alto", event.getAlerta());
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de HDOP Alto via WebSocket: {}", e.getMessage());
        }
    }
}