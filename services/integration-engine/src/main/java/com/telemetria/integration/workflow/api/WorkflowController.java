package com.telemetria.integration.workflow.api;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.workflow.domain.ViagemWorkflowRequest;
import com.telemetria.integration.workflow.domain.ViagemWorkflowResponse;
import com.telemetria.integration.workflow.route.InicioViagemWorkflowRoute;

@RestController
@RequestMapping("/api/integracoes/workflow")
public class WorkflowController {

    private final ProducerTemplate producerTemplate;

    public WorkflowController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PostMapping("/iniciar-viagem")
    public ResponseEntity<ViagemWorkflowResponse> iniciarViagem(@RequestBody ViagemWorkflowRequest request) {
        ViagemWorkflowResponse response = producerTemplate.requestBody(
                InicioViagemWorkflowRoute.ROUTE_INICIAR_VIAGEM,
                request,
                ViagemWorkflowResponse.class
        );
        return ResponseEntity.ok(response);
    }
}
