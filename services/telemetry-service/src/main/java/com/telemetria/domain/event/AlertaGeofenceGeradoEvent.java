package com.telemetria.domain.event;

import org.springframework.context.ApplicationEvent;

import com.telemetria.domain.entity.Alerta;

public class AlertaGeofenceGeradoEvent extends ApplicationEvent {
    private final Alerta alerta;

    public AlertaGeofenceGeradoEvent(Object source, Alerta alerta) {
        super(source);
        this.alerta = alerta;
    }

    public Alerta getAlerta() {
        return alerta;
    }
}