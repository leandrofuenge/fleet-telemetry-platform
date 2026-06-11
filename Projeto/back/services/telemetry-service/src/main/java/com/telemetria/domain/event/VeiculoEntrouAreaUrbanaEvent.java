package com.telemetria.domain.event;

import org.springframework.context.ApplicationEvent;

public class VeiculoEntrouAreaUrbanaEvent extends ApplicationEvent {

    private final Long veiculoId;
    private final String placaVeiculo;
    private final String mensagem;

    public VeiculoEntrouAreaUrbanaEvent(Object source, Long veiculoId, String placaVeiculo, String mensagem) {
        super(source);
        this.veiculoId = veiculoId;
        this.placaVeiculo = placaVeiculo;
        this.mensagem = mensagem;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public String getMensagem() {
        return mensagem;
    }
}