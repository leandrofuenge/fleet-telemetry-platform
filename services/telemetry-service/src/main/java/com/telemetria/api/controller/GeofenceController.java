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

import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.GeofenceService;
import com.telemetria.infrastructure.persistence.GeofenceRepository;

/** Cadastro de geofences com validação geométrica antes da persistência. */
@RestController
@RequestMapping("/api/v1/geofences")
public class GeofenceController {
    private final GeofenceRepository repository;
    private final GeofenceService service;

    public GeofenceController(GeofenceRepository repository, GeofenceService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping("/tenant/{tenantId}")
    public List<Geofence> listar(@PathVariable Long tenantId) {
        return repository.findByTenantIdAndAtivoTrue(tenantId);
    }

    @PostMapping
    public ResponseEntity<Geofence> criar(@RequestBody Geofence geofence) {
        service.validarDefinicao(geofence);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(geofence));
    }

    @PutMapping("/{id}")
    public Geofence atualizar(@PathVariable Long id, @RequestBody Geofence dados) {
        Geofence atual = repository.findById(id).orElseThrow(() ->
                new BusinessException(ErrorCode.VALIDATION_ERROR, "Geofence não encontrada"));
        if (!atual.getTenantId().equals(dados.getTenantId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tenant da geofence não pode ser alterado");
        }
        dados.setId(id);
        if (dados.getUuid() == null) dados.setUuid(atual.getUuid());
        service.validarDefinicao(dados);
        return repository.save(dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        Geofence geofence = repository.findById(id).orElseThrow(() ->
                new BusinessException(ErrorCode.VALIDATION_ERROR, "Geofence não encontrada"));
        geofence.setAtivo(false);
        repository.save(geofence);
        return ResponseEntity.noContent().build();
    }
}
