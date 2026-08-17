package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaVelocidadeImpossivelGeradoEvent;

@Component
public class AlertaVelocidadeImpossivelEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaVelocidadeImpossivelEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    // Construtor manual seguro contra falhas de compilação da IDE
    public AlertaVelocidadeImpossivelEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background, liberando a thread principal de telemetria
    @EventListener
    public void handleVelocidadeImpossivelGerado(AlertaVelocidadeImpossivelGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo alerta de velocidade impossível ID: {}", event.getAlerta().getId());
            
            // Ajuste o tópico conforme o padrão do seu front-end (ex: /topic/alertas/criticos ou geral)
            messagingTemplate.convertAndSend("/topic/alertas/velocidade-impossivel", event.getAlerta());
            
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de velocidade impossível via WebSocket: {}", e.getMessage());
        }
    }
}