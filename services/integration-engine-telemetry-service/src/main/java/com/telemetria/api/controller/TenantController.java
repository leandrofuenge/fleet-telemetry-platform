package com.telemetria.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Tenant;
import com.telemetria.domain.service.TenantService;
import com.telemetria.infrastructure.persistence.TenantRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private final TenantService tenantService;
    private final TenantRepository tenantRepository;

    public TenantController(TenantService tenantService, TenantRepository tenantRepository) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping
    public ResponseEntity<Tenant> criar(@Valid @RequestBody Tenant tenant) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.criar(tenant));
    }

    @GetMapping
    public List<Tenant> listar() { return tenantRepository.findAll(); }

    @GetMapping("/{id}")
    public Tenant buscar(@PathVariable Long id) { return tenantService.buscarObrigatorio(id); }
}
