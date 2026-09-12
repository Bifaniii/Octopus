package com.br.octopus_msusuario.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AuxiliarRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Size(max = 20) String telefone
) {
}
