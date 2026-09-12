package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String email,
        Role role,
        boolean ativo,
        LocalDateTime criadoEm
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getCriadoEm()
        );
    }
}
