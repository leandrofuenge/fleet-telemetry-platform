package com.telemetria.domain.event;

import org.springframework.context.ApplicationEvent;

import com.telemetria.domain.entity.Alerta;

/**
 * SÊNIOR: Evento de domínio para desacoplamento de alertas de velocidade impossível.
 */
public class AlertaVelocidadeImpossivelGeradoEvent extends ApplicationEvent {
    
    private final Alerta alerta;

    public AlertaVelocidadeImpossivelGeradoEvent(Object source, Alerta alerta) {
        super(source);
        this.alerta = alerta;
    }

    public Alerta getAlerta() {
        return alerta;
    }
}