package com.telemetria.integration.senatran.serpro.api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.senatran.serpro.application.SerproConsultaService;
import com.telemetria.integration.senatran.serpro.domain.InvalidVehicleQueryException;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoRequest;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoResponse;

@RestController
@RequestMapping("/api/integracoes/senatran/serpro")
public class SerproConsultaController {
    private final SerproConsultaService service;

    public SerproConsultaController(SerproConsultaService service) { this.service = service; }

    @PostMapping("/veiculos/consulta")
    public ResponseEntity<SerproVeiculoResponse> consultar(@RequestBody SerproVeiculoRequest request) {
        if (request == null) throw new InvalidVehicleQueryException("Dados da consulta não informados.");
        return ResponseEntity.ok(service.consultarVeiculo(request.placa(), request.renavam()));
    }
}
