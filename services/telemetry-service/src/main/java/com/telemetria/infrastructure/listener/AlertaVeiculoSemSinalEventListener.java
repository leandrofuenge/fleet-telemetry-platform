package com.telemetria.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.event.AlertaVeiculoSemSinalGeradoEvent;

@Component
public class AlertaVeiculoSemSinalEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaVeiculoSemSinalEventListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public AlertaVeiculoSemSinalEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async // 🚀 Executa em background para manter a velocidade do Job de infraestrutura
    @EventListener
    public void handleVeiculoSemSinalGerado(AlertaVeiculoSemSinalGeradoEvent event) {
        try {
            log.debug("📡 [WebSocket Despache] Transmitindo timeout de sinal do veículo ID: {}", event.getAlerta().getVeiculoId());
            messagingTemplate.convertAndSend("/topic/alertas/veiculo-sem-sinal", event.getAlerta());
        } catch (Exception e) {
            log.error("❌ Falha ao transmitir alerta de falta de sinal via WebSocket: {}", e.getMessage());
        }
    }
}