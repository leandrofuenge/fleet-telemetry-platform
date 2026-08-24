package com.telemetria.infrastructure.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // 🟢 ADICIONADO
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate; // 🟢 ADICIONADO
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.event.AlertaGeradoEvent;
import com.telemetria.domain.event.AlertasResolvidosEvent;
import com.telemetria.domain.event.VeiculoEntrouAreaUrbanaEvent; // 🟢 CORRIGIDO: Import do novo evento

@Component
public class AlertaEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaEventListener.class);

    // 🟢 CORRIGIDO: Descomentado e injetado via @Autowired para o WebSocket funcionar
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Escuta o evento de alerta gerado.
     * O @Async garante que o envio do WebSocket rode em um pool de threads separado.
     */
    @Async
    @EventListener
    public void handleAlertaGerado(AlertaGeradoEvent event) {
        Alerta alerta = event.getAlerta();
        String threadName = Thread.currentThread().getName();
        
        log.debug("📡 [Thread: {}] Evento interceptado. Despachando alerta ID {} via WebSocket...", threadName, alerta.getId());
        
        try {
            // 🟢 ATIVADO: Agora envia o alerta real mapeado para o tópico do front-end
            messagingTemplate.convertAndSend("/topic/alertas", alerta);
            
            log.info("✅ [Thread: {}] Alerta ID {} enviado com sucesso via WebSocket", threadName, alerta.getId());
        } catch (Exception e) {
            log.error("❌ [Thread: {}] Erro ao enviar alerta ID {} via WebSocket: {}", threadName, alerta.getId(), e.getMessage());
        }
    }
    
    /**
     * Escuta o evento de alertas resolvidos em lote.
     * Envia um comando limpo para o front-end atualizar a tela.
     */
    @Async
    @EventListener
    public void handleAlertasResolvidos(AlertasResolvidosEvent event) {
        String threadName = Thread.currentThread().getName();
        log.debug("📡 [Thread: {}] Enviando sinal de alertas resolvidos para o veículo {}", 
                threadName, event.getVeiculoId());
        
        try {
            String destino = "/topic/alertas/resolvidos";
            
            // 🟢 ATIVADO: Avisa o front-end qual lote de veículo foi limpo
            messagingTemplate.convertAndSend(destino, event);
            
            log.info("✅ [Thread: {}] Notificação de resolução (Veículo: {}, Tipo: {}) enviada via WebSocket", 
                    threadName, event.getVeiculoId(), event.getTipoAlerta());
        } catch (Exception e) {
            log.error("❌ [Thread: {}] Erro ao enviar resolução via WebSocket: {}", threadName, e.getMessage());
        }
    }
    
    /**
     * Escuta a transição de entrada em perímetro urbano.
     */
    @Async
    @EventListener
    public void handleVeiculoEntrouAreaUrbana(VeiculoEntrouAreaUrbanaEvent event) {
        String threadName = Thread.currentThread().getName();
        log.debug("📡 [Thread: {}] Despachando notificação de perímetro urbano para o veículo {}", 
                threadName, event.getPlacaVeiculo());
        
        try {
            // 🟢 CORRIGIDO: Tratado o log de erro interno para pegar o nome da Thread correto se falhar
            messagingTemplate.convertAndSend("/topic/alertas", event.getMensagem());
            log.info("✅ [Thread: {}] Notificação de área urbana do veículo {} enviada com sucesso", threadName, event.getPlacaVeiculo());
        } catch (Exception e) {
            log.error("❌ [Thread: {}] Erro ao enviar WebSocket de perímetro: {}", threadName, e.getMessage());
        }
    }
}