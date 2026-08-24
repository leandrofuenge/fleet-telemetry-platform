package com.telemetria.infrastructure.listener;

// Importações manuais para substituir o Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaGeofenceGeradoEvent;

@Component
public class AlertaGeofenceEventListener {

    // 1. Instanciação manual do Logger (Corrige o erro "log cannot be resolved")
    private static final Logger log = LoggerFactory.getLogger(AlertaGeofenceEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    // 2. Construtor manual (Corrige o erro "messagingTemplate may not have been initialized")
    public AlertaGeofenceEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em uma Thread pool separada. Não trava o fluxo de processamento de GPS!
    @EventListener
    public void handleAlertaGeofenceGerado(AlertaGeofenceGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de geofence ID: {}", event.getAlerta().getId());
            messagingTemplate.convertAndSend("/topic/alertas/geofence", event.getAlerta());
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de geofence via WebSocket: {}", e.getMessage());
        }
    }
}