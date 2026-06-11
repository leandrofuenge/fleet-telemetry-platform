package com.telemetria.infrastructure.messaging.dto;

import java.time.LocalDateTime;

import com.telemetria.domain.entity.Alerta;

public record AlertaWebSocketDTO(
    Long id,
    Long veiculoId,
    String tipo,
    String severidade,
    String mensagem,
    LocalDateTime dataHora,
    Double latitude,
    Double longitude
) {
    // Factory method para converter a Entidade em DTO de forma limpa
    public static AlertaWebSocketDTO de(Alerta alerta) {
        return new AlertaWebSocketDTO(
            alerta.getId(),
            alerta.getVeiculoId(),
            alerta.getTipo() != null ? alerta.getTipo().name() : "OUTRO",
            alerta.getSeveridade() != null ? alerta.getSeveridade().name() : "BAIXA",
            alerta.getMensagem(),
            alerta.getDataHora(),
            alerta.getLatitude(),
            alerta.getLongitude()
        );
    }
}