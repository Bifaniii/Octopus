package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Admin;

import java.util.UUID;

public record AdminResponse(
        UUID id,
        String nome,
        UsuarioResponse usuario
) {
    public static AdminResponse from(Admin admin) {
        return new AdminResponse(
                admin.getId(),
                admin.getNome(),
                UsuarioResponse.from(admin.getUsuario())
        );
    }
}
