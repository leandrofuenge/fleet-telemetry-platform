package com.telemetria.api.dto.response;

import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.enums.Perfil;

public record UsuarioResponse(Long id, Long tenantId, String login, String nome, String email,
        Perfil perfil, Boolean ativo, Boolean mfaAtivado) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getTenantId(), usuario.getLogin(), usuario.getNome(),
                usuario.getEmail(), usuario.getPerfil(), usuario.getAtivo(), usuario.getMfaAtivado());
    }
}
