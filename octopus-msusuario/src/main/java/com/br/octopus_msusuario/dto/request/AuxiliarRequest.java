package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.validation.Celular;
import com.br.octopus_msusuario.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AuxiliarRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String senha,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Cpf String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Celular String telefone
) {
}
