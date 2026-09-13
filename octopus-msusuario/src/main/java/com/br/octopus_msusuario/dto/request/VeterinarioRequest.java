package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VeterinarioRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 dígitos (CPF) ou 14 dígitos (CNPJ)") String cpfCnpj,
        @NotNull TipoPessoa tipoPessoa,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Size(max = 20) String telefone,
        @NotBlank @Size(max = 20) String crmv,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "deve ser a sigla da UF com 2 letras maiúsculas") String crmvUf,
        Especializacao especializacao
) {
}
