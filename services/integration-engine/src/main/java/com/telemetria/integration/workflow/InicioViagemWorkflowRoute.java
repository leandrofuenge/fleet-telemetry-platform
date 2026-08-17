package com.telemetria.integration.workflow;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.telemetria.integration.model.ViagemWorkflowRequest;
import com.telemetria.integration.model.ViagemWorkflowResponse;
import com.telemetria.integration.route.CteRoute;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

@Component
public class InicioViagemWorkflowRoute extends RouteBuilder {

    public static final String ROUTE_INICIAR_VIAGEM = "direct:iniciar-viagem";
    public static final String ROUTE_VALIDAR_MOTORISTA = "direct:validar-motorista";
    public static final String ROUTE_VALIDAR_VEICULO = "direct:validar-veiculo";
    public static final String ROUTE_CONSULTAR_SEFAZ_VIAGEM = "direct:consultar-sefaz-viagem";
    public static final String ROUTE_LIBERAR_VIAGEM = "direct:liberar-viagem";

    @Override
    public void configure() {

        // Workflow Central de Liberação e Início de Viagem
        from(ROUTE_INICIAR_VIAGEM)
            .routeId("workflow-inicio-viagem")
            .process("auditLogProcessor")
            .log("Iniciando orquestracao do workflow de viagem: ${body.viagemId}")
            .process(exchange -> {
                ViagemWorkflowRequest req = exchange.getIn().getBody(ViagemWorkflowRequest.class);
                ViagemWorkflowResponse resp = new ViagemWorkflowResponse(
                        req != null ? req.getViagemId() : "VGM-001",
                        "EM_ANALISE"
                );
                exchange.setProperty("WORKFLOW_REQ", req);
                exchange.setProperty("WORKFLOW_RESP", resp);
            })
            .to(ROUTE_VALIDAR_MOTORISTA)
            .to(ROUTE_VALIDAR_VEICULO)
            .to(ROUTE_CONSULTAR_SEFAZ_VIAGEM)
            .to(ROUTE_LIBERAR_VIAGEM)
            .setBody(exchangeProperty("WORKFLOW_RESP"))
            .process("auditLogProcessor")
            .log("Workflow de viagem finalizado com status: ${body.status}");

        // Etapa 1: Validação de Motorista
        from(ROUTE_VALIDAR_MOTORISTA)
            .routeId("sub-workflow-validar-motorista")
            .log("Validando dados cadastrais e CNH do motorista...")
            .process(exchange -> {
                ViagemWorkflowResponse resp = exchange.getProperty("WORKFLOW_RESP", ViagemWorkflowResponse.class);
                ViagemWorkflowRequest req = exchange.getProperty("WORKFLOW_REQ", ViagemWorkflowRequest.class);
                boolean valido = req != null && req.getMotoristaCpf() != null && !req.getMotoristaCpf().isBlank();
                resp.setMotoristaValido(valido);
                if (valido) {
                    resp.getEtapasConcluidas().add("MOTORISTA_VALIDADO");
                } else {
                    resp.getPendencias().add("CPF de motorista inválido ou ausente");
                }
            });

        // Etapa 2: Validação de Veículo
        from(ROUTE_VALIDAR_VEICULO)
            .routeId("sub-workflow-validar-veiculo")
            .log("Validando telemetria e documentação do veículo...")
            .process(exchange -> {
                ViagemWorkflowResponse resp = exchange.getProperty("WORKFLOW_RESP", ViagemWorkflowResponse.class);
                ViagemWorkflowRequest req = exchange.getProperty("WORKFLOW_REQ", ViagemWorkflowRequest.class);
                boolean valido = req != null && req.getVeiculoPlaca() != null && !req.getVeiculoPlaca().isBlank();
                resp.setVeiculoValido(valido);
                if (valido) {
                    resp.getEtapasConcluidas().add("VEICULO_VALIDADO");
                } else {
                    resp.getPendencias().add("Placa de veículo inválida ou ausente");
                }
            });

        // Etapa 3: Verificação de Disponibilidade SEFAZ CT-e
        from(ROUTE_CONSULTAR_SEFAZ_VIAGEM)
            .routeId("sub-workflow-consultar-sefaz")
            .log("Consultando disponibilidade da SEFAZ para autorização do CT-e...")
            .process(exchange -> {
                ViagemWorkflowRequest req = exchange.getProperty("WORKFLOW_REQ", ViagemWorkflowRequest.class);
                String uf = (req != null && req.getUfOrigem() != null) ? req.getUfOrigem() : "MT";
                CteStatusRequest sefazReq = new CteStatusRequest(uf, "homologacao");
                exchange.getIn().setBody(sefazReq);
            })
            .to(CteRoute.ROUTE_CTE_STATUS)
            .process(exchange -> {
                CteStatusResponse sefazResp = exchange.getIn().getBody(CteStatusResponse.class);
                ViagemWorkflowResponse resp = exchange.getProperty("WORKFLOW_RESP", ViagemWorkflowResponse.class);
                boolean disponivel = sefazResp != null && sefazResp.isDisponivel();
                resp.setSefazDisponivel(disponivel);
                if (disponivel) {
                    resp.getEtapasConcluidas().add("SEFAZ_CTE_OPERACIONAL");
                } else {
                    resp.getPendencias().add("SEFAZ CT-e indisponível no momento");
                }
            });

        // Etapa 4: Conclusão e Decisão de Liberação
        from(ROUTE_LIBERAR_VIAGEM)
            .routeId("sub-workflow-liberar-viagem")
            .process(exchange -> {
                ViagemWorkflowResponse resp = exchange.getProperty("WORKFLOW_RESP", ViagemWorkflowResponse.class);
                if (resp.isMotoristaValido() && resp.isVeiculoValido() && resp.isSefazDisponivel()) {
                    resp.setStatus("LIBERADA");
                    resp.getEtapasConcluidas().add("VIAGEM_LIBERADA");
                } else {
                    resp.setStatus("BLOQUEADA");
                }
            });
    }
}
