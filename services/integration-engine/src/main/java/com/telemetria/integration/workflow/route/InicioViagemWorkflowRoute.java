package com.telemetria.integration.workflow.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.telemetria.integration.workflow.application.InicioViagemService;
import com.telemetria.integration.workflow.domain.ViagemWorkflowRequest;

@Component
public class InicioViagemWorkflowRoute extends RouteBuilder {

    public static final String ROUTE_INICIAR_VIAGEM = "direct:iniciar-viagem";
    private final InicioViagemService inicioViagemService;

    public InicioViagemWorkflowRoute(InicioViagemService inicioViagemService) {
        this.inicioViagemService = inicioViagemService;
    }

    @Override
    public void configure() {

        from(ROUTE_INICIAR_VIAGEM)
            .routeId("workflow-inicio-viagem")
            .process("auditLogProcessor")
            .log("Iniciando orquestracao do workflow de viagem: ${body.viagemId}")
            .process(exchange -> exchange.getMessage().setBody(
                    inicioViagemService.executar(exchange.getMessage().getBody(ViagemWorkflowRequest.class))))
            .process("auditLogProcessor")
            .log("Workflow de viagem finalizado com status: ${body.status}");
    }
}
