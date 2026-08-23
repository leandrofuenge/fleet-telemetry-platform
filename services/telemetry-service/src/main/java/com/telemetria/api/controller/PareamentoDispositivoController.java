package com.telemetria.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.PareamentoDispositivoService;
import com.telemetria.infrastructure.persistence.UsuarioRepository;

@RestController
@RequestMapping("/api/v1/dispositivos/pareamentos")
public class PareamentoDispositivoController {
    private final PareamentoDispositivoService pareamentoService;
    private final UsuarioRepository usuarios;

    public PareamentoDispositivoController(PareamentoDispositivoService pareamentoService, UsuarioRepository usuarios) {
        this.pareamentoService = pareamentoService;
        this.usuarios = usuarios;
    }

    @PostMapping
    public ResponseEntity<PareamentoDispositivoService.CodigoPareamento> criar(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long veiculoId,
            @RequestParam(required = false) Integer expiraEmMinutos,
            Authentication authentication) {
        Usuario usuario = usuarios.findByLogin(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (usuario.getTenantId() != null && !usuario.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Não é permitido criar código para outro tenant.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(pareamentoService.criar(tenantId, veiculoId, expiraEmMinutos));
    }

    @PostMapping("/consumir")
    public ResponseEntity<DispositivoIot> consumir(
            @RequestParam String deviceId,
            @RequestParam String codigo) {
        return ResponseEntity.ok(pareamentoService.consumir(deviceId, codigo));
    }
}
