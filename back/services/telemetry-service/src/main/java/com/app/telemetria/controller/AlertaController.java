package com.app.telemetria.controller;

import com.app.telemetria.entity.Alerta;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.repository.AlertaRepository;
import com.app.telemetria.service.AlertaService;
import com.app.telemetria.service.VeiculoService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    public Page<Alerta> listarTodos(Pageable pageable) {
        return alertaService.listarTodos(pageable);
    }

    @GetMapping("/ativos")
    public List<Alerta> listarAtivos() {
        return alertaService.listarAtivos();
    }

    @GetMapping("/veiculo/{veiculoId}")
    public List<Alerta> listarPorVeiculo(@PathVariable Long veiculoId) {
        return alertaService.listarPorVeiculo(veiculoId);
    }

    @GetMapping("/motorista/{motoristaId}")
    public List<Alerta> listarPorMotorista(@PathVariable Long motoristaId) {
        return alertaService.listarPorMotorista(motoristaId);
    }

    @GetMapping("/viagem/{viagemId}")
    public List<Alerta> listarPorViagem(@PathVariable Long viagemId) {
        return alertaService.listarPorViagem(viagemId);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return alertaService.dashboard();
    }

    @PutMapping("/{id}/ler")
    public Alerta marcarComoLido(@PathVariable Long id) {
        return alertaService.marcarComoLido(id);
    }

    @PutMapping("/{id}/resolver")
    public Alerta resolverAlerta(@PathVariable Long id) {
        return alertaService.resolverAlerta(id);
    }

    @GetMapping("/periodo")
    public List<Alerta> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime inicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fim) {

        return alertaService.listarPorPeriodo(inicio, fim);
    }
}