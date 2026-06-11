package com.telemetria.domain.event;

import org.springframework.context.ApplicationEvent;

import com.telemetria.domain.entity.Alerta;

/**
 * SÊNIOR: Evento de domínio para desacoplamento de alertas de score crítico em viagens.
 */
public class AlertaScoreCriticoViagemGeradoEvent extends ApplicationEvent {
    
    private final Alerta alerta;

    public AlertaScoreCriticoViagemGeradoEvent(Object source, Alerta alerta) {
        super(source);
        this.alerta = alerta;
    }

    public Alerta getAlerta() {
        return alerta;
    }
}