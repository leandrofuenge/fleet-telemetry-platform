package com.telemetria.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.api.dto.request.TrocaDispositivoRequest;
import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.infrastructure.persistence.UsuarioRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/veiculos/{veiculoId}/dispositivo-principal")
public class DispositivoController {
    private final AlertaService alertaService;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;

    public DispositivoController(AlertaService alertaService, UsuarioRepository usuarioRepository,
            VeiculoRepository veiculoRepository) {
        this.alertaService = alertaService;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @PostMapping
    public ResponseEntity<Void> trocarPrincipal(@PathVariable Long veiculoId,
            @Valid @RequestBody TrocaDispositivoRequest request, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEICULO_NOT_FOUND));
        if (usuario.getTenantId() != null && !usuario.getTenantId().equals(veiculo.getTenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Veículo pertence a outro tenant.");
        }
        alertaService.trocarDispositivo(veiculoId, request.getDeviceId(), request.getOdometroAtualKm(), usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
