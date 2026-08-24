package com.telemetria.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.api.dto.request.CriarUsuarioRequest;
import com.telemetria.api.dto.response.UsuarioResponse;
import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setTenantId(request.getTenantId());
        usuario.setLogin(request.getLogin());
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setCpf(request.getCpf().replaceAll("\\D", ""));
        usuario.setPerfil(request.getPerfil());
        Usuario salvo = usuarioService.criar(usuario, request.getSenha());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(salvo));
    }
}
