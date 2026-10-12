package com.br.octopus_msrelatorios.client.dto;

// GET /api/usuarios/nomes do msusuario: liga o e-mail gravado nos outros módulos ao nome da pessoa.
public record UsuarioNomeDto(String email, String nome) {
}
