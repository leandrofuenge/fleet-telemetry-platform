package com.telemetria.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.RegraAlerta;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.RegraAlertaService;
import com.telemetria.infrastructure.persistence.RegraAlertaRepository;

@RestController
@RequestMapping("/api/v1/regras-alerta")
public class RegraAlertaController {
    private final RegraAlertaService service; private final RegraAlertaRepository repository;
    public RegraAlertaController(RegraAlertaService service, RegraAlertaRepository repository) { this.service = service; this.repository = repository; }
    @GetMapping("/tenant/{tenantId}") public List<RegraAlerta> listar(@PathVariable Long tenantId) { return service.listar(tenantId); }
    @PostMapping public ResponseEntity<RegraAlerta> criar(@RequestBody RegraAlerta regra) { return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(regra)); }
    @PutMapping("/{id}") public RegraAlerta atualizar(@PathVariable Long id, @RequestBody RegraAlerta regra) {
        RegraAlerta atual = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Regra não encontrada"));
        if (!atual.getTenantId().equals(regra.getTenantId())) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tenant da regra não pode ser alterado");
        regra.setId(id); return service.salvar(regra);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> desativar(@PathVariable Long id) {
        RegraAlerta regra = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Regra não encontrada"));
        regra.setAtivo(false); service.salvar(regra); return ResponseEntity.noContent().build();
    }
}
