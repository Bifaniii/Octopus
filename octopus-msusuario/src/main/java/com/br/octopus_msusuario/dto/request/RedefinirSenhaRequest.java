package com.br.octopus_msusuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(

        @NotBlank(message = "O código é obrigatório.")
        String token,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, max = 100, message = "A senha deve ter no mínimo 6 caracteres.")
        String novaSenha
) {
}
