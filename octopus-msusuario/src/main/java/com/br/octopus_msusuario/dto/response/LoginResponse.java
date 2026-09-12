package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.enums.Role;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        String email,
        Role role
) {
}
