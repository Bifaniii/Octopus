package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.validation.NomeCompleto;
import com.br.octopus_msusuario.validation.Celular;
import com.br.octopus_msusuario.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Edição de auxiliar e de recepcionista, que têm os mesmos campos. Sem senha.
public record ColaboradorUpdateRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) @NomeCompleto String nome,
        @NotBlank @Cpf String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Celular String telefone
) {
}
