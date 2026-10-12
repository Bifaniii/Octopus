package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.enums.Role;

// Os outros módulos gravam o e-mail de quem fez cada operação; com isto eles mostram o nome.
public record UsuarioNomeResponse(String email, String nome, Role role) {
}
