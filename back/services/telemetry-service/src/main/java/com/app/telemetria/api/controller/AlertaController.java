package com.app.telemetria.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.telemetria.api.dto.response.AlertaResponseDTO; // Este é usado
import com.app.telemetria.domain.entity.Alerta;
import com.app.telemetria.domain.service.AlertaService;

// TODOS OS IMPORTS ACIMA SÃO USADOS - NÃO REMOVER




@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private static final Logger log = LoggerFactory.getLogger(AlertaController.class);
    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
        log.info("✅ AlertaController inicializado");
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<AlertaResponseDTO> listarTodos(
            @PageableDefault(page = 0, size = 10, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("📋 Listando todos alertas - página: {}, tamanho: {}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Alerta> alertas = alertaService.listarTodos(pageable);
        
        // Converte para DTO
        List<AlertaResponseDTO> dtos = alertas.getContent()
                .stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        Page<AlertaResponseDTO> resultado = new PageImpl<>(dtos, pageable, alertas.getTotalElements());
        
        log.debug("✅ Total de alertas encontrados: {}", resultado.getTotalElements());
        return resultado;
    }

    @GetMapping("/ativos")
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAtivos() {
        log.debug("🔴 Buscando alertas ativos (não resolvidos)");
        
        List<Alerta> alertas = alertaService.listarAtivos();
        
        List<AlertaResponseDTO> resultado = alertas.stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        log.debug("✅ Alertas ativos encontrados: {}", resultado.size());
        return resultado;
    }

    @GetMapping("/veiculo/{veiculoId}")
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPorVeiculo(@PathVariable Long veiculoId) {
        log.debug("🚛 Buscando alertas para veículo ID: {}", veiculoId);
        
        List<Alerta> alertas = alertaService.listarPorVeiculo(veiculoId);
        
        List<AlertaResponseDTO> resultado = alertas.stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        log.debug("✅ Alertas encontrados para veículo {}: {}", veiculoId, resultado.size());
        return resultado;
    }

    @GetMapping("/motorista/{motoristaId}")
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPorMotorista(@PathVariable Long motoristaId) {
        log.debug("👤 Buscando alertas para motorista ID: {}", motoristaId);
        
        List<Alerta> alertas = alertaService.listarPorMotorista(motoristaId);
        
        List<AlertaResponseDTO> resultado = alertas.stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        log.debug("✅ Alertas encontrados para motorista {}: {}", motoristaId, resultado.size());
        return resultado;
    }

    @GetMapping("/viagem/{viagemId}")
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPorViagem(@PathVariable Long viagemId) {
        log.debug("🛣️ Buscando alertas para viagem ID: {}", viagemId);
        
        List<Alerta> alertas = alertaService.listarPorViagem(viagemId);
        
        List<AlertaResponseDTO> resultado = alertas.stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        log.debug("✅ Alertas encontrados para viagem {}: {}", viagemId, resultado.size());
        return resultado;
    }

    @GetMapping("/periodo")
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        
        log.debug("📅 Buscando alertas entre {} e {}", inicio, fim);
        
        List<Alerta> alertas = alertaService.listarPorPeriodo(inicio, fim);
        
        List<AlertaResponseDTO> resultado = alertas.stream()
                .map(AlertaResponseDTO::new)
                .collect(Collectors.toList());
        
        log.debug("✅ Alertas encontrados no período: {}", resultado.size());
        return resultado;
    }

    // Métodos PUT continuam retornando a entidade (não precisam de DTO)
    @PutMapping("/{id}/ler")
    @Transactional
    public Alerta marcarComoLido(@PathVariable Long id) {
        log.debug("👁️ Marcando alerta {} como lido", id);
        
        Alerta resultado = alertaService.marcarComoLido(id);
        
        log.debug("✅ Alerta {} marcado como lido", id);
        return resultado;
    }

    @PutMapping("/{id}/resolver")
    @Transactional
    public Alerta resolverAlerta(@PathVariable Long id) {
        log.debug("✅ Resolvendo alerta {}", id);
        
        Alerta resultado = alertaService.resolverAlerta(id);
        
        log.debug("✅ Alerta {} resolvido com sucesso", id);
        return resultado;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        log.debug("📊 Gerando dashboard de alertas");
        
        Map<String, Object> resultado = alertaService.dashboard();
        
        log.debug("✅ Dashboard gerado: total ativos={}", resultado.get("totalAtivos"));
        return resultado;
    }
}