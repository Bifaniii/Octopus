package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;

// Linha de GET /api/usuarios do msusuario (só ADMIN). role vem como ROLE_*.
public record UsuarioDto(String nome, String email, String role, boolean ativo, LocalDateTime criadoEm) {
}
