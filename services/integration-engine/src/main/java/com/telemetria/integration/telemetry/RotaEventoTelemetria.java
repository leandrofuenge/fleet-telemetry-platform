package com.telemetria.integration.telemetry;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.telemetria.integration.support.AuditLogProcessor;

/** Rota de extensão para ações externas desencadeadas por telemetria persistida. */
@Component
public class RotaEventoTelemetria extends RouteBuilder {

    public static final String ROUTE_PROCESSAR_EVENTO = "direct:telemetria-integration-event";

    @Override
    public void configure() {
        from(ROUTE_PROCESSAR_EVENTO)
                .routeId("telemetria-integration-event-route")
                .process("auditLogProcessor")
                .log("Evento de telemetria recebido para integração: eventId=${body.eventId}, "
                        + "tenantId=${body.tenantId}, veiculoId=${body.veiculoId}")
                .process("auditLogProcessor");
    }
}
