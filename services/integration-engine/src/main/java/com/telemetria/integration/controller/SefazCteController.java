package com.telemetria.integration.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.route.CteRoute;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

@RestController
@RequestMapping("/api/integracoes/sefaz/cte")
public class SefazCteController {

    private final ProducerTemplate producerTemplate;

    public SefazCteController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @GetMapping("/status")
    public ResponseEntity<CteStatusResponse> consultarStatus(
            @RequestParam(required = false, defaultValue = "MT") String uf,
            @RequestParam(required = false) String ambiente) {

        CteStatusRequest request = new CteStatusRequest(uf, ambiente);
        CteStatusResponse response = producerTemplate.requestBody(
                CteRoute.ROUTE_CTE_STATUS,
                request,
                CteStatusResponse.class
        );

        return ResponseEntity.ok(response);
    }
}
