package com.telemetria.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.ApiClient;
import com.telemetria.domain.entity.VrpJob;
import com.telemetria.domain.entity.Webhook;
import com.telemetria.domain.service.ApiIntegracaoService;
import com.telemetria.domain.service.VrpService;
import com.telemetria.infrastructure.integration.engine.SerproIntegrationClient;
import com.telemetria.infrastructure.integration.engine.SerproIntegrationClient.SerproIntegrationUnavailableException;
import com.telemetria.infrastructure.integration.engine.dto.SerproVeiculoConsultaRequest;
import com.telemetria.infrastructure.integration.engine.dto.SerproVeiculoConsultaResponse;

@RestController
@RequestMapping("/api/v1/integracoes")
public class IntegracaoController {
    private final VrpService vrpService;
    private final ApiIntegracaoService apiIntegracaoService;
    private final SerproIntegrationClient serproIntegrationClient;

    public IntegracaoController(VrpService vrpService, ApiIntegracaoService apiIntegracaoService,
            SerproIntegrationClient serproIntegrationClient) {
        this.vrpService = vrpService;
        this.apiIntegracaoService = apiIntegracaoService;
        this.serproIntegrationClient = serproIntegrationClient;
    }

    @PostMapping("/vrp")
    public ResponseEntity<VrpJob> criarVrp(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "CVRP") String tipo,
            @RequestParam int veiculos,
            @RequestParam int pontos) {
        return ResponseEntity.accepted().body(vrpService.criar(tenantId, tipo, veiculos, pontos));
    }

    @GetMapping("/vrp/{id}")
    public VrpJob buscarVrp(@PathVariable Long id) {
        return vrpService.buscar(id);
    }

    @PostMapping("/api-clients")
    public ResponseEntity<ApiIntegracaoService.Credencial> criarCliente(@RequestBody ApiClient client) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiIntegracaoService.criarCliente(client));
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Webhook> registrarWebhook(
            @RequestBody Webhook webhook,
            @RequestParam String segredo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiIntegracaoService.registrarWebhook(webhook, segredo));
    }

    @PostMapping("/senatran/serpro/veiculos/consulta")
    public ResponseEntity<SerproVeiculoConsultaResponse> consultarVeiculoSerpro(
            @RequestBody SerproVeiculoConsultaRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        if (request == null || request.placa() == null || request.placa().isBlank()
                || request.renavam() == null || request.renavam().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(serproIntegrationClient.consultarVeiculo(request, correlationId));
        } catch (SerproIntegrationUnavailableException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
