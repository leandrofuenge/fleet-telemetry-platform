package com.telemetria.api.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Jornada;
import com.telemetria.domain.enums.OrigemDado;
import com.telemetria.domain.service.JornadaService;
@RestController
@RequestMapping("/api/v1/jornadas")
public class JornadaController {
    private final JornadaService jornadaService;

    public JornadaController(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    @PostMapping
    public Jornada iniciar(@RequestParam Long tenantId, @RequestParam Long motoristaId,
            @RequestParam Long veiculoId, @RequestParam(required = false) Long viagemId) {
        return jornadaService.iniciar(tenantId, motoristaId, veiculoId, viagemId, OrigemDado.MANUAL);
    }

    @PostMapping("/motorista/{motoristaId}/fechar")
    public Jornada fechar(@PathVariable Long motoristaId) {
        return jornadaService.fechar(motoristaId);
    }
}
