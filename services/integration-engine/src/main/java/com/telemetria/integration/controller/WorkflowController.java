package com.telemetria.integration.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.model.ViagemWorkflowRequest;
import com.telemetria.integration.model.ViagemWorkflowResponse;
import com.telemetria.integration.workflow.InicioViagemWorkflowRoute;

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
