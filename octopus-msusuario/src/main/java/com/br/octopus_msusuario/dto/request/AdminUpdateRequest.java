package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.validation.NomeCompleto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) @NomeCompleto String nome
) {
}
