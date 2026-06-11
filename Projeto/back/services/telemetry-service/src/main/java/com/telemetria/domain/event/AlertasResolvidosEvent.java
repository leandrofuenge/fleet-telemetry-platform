package com.telemetria.domain.event;

import org.springframework.context.ApplicationEvent;

import com.telemetria.domain.enums.TipoAlerta; // 🔴 CORRIGIDO: Apontando para o pacote de enums

public class AlertasResolvidosEvent extends ApplicationEvent {

    private final Long veiculoId;
    private final TipoAlerta tipoAlerta;

    public AlertasResolvidosEvent(Object source, Long veiculoId, TipoAlerta tipoAlerta) {
        super(source);
        this.veiculoId = veiculoId;
        this.tipoAlerta = tipoAlerta;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public TipoAlerta getTipoAlerta() {
        return tipoAlerta;
    }
}