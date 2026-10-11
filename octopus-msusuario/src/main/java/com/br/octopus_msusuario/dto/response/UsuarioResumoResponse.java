package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

// Linha da listagem de usuários: id da conta (Usuario) e id do perfil, que é o usado em /api/<perfil>/{id}.
public record UsuarioResumoResponse(
        UUID id,
        UUID perfilId,
        String nome,
        String email,
        Role role,
        boolean ativo,
        LocalDateTime criadoEm
) {
}
