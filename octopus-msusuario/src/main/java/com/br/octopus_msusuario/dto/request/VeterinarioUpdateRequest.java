package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import com.br.octopus_msusuario.domain.enums.UnidadeFederativa;
import com.br.octopus_msusuario.validation.Celular;
import com.br.octopus_msusuario.validation.DadosDocumento;
import com.br.octopus_msusuario.validation.DocumentoCompativel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Mesmos campos e regras do cadastro, sem a senha: ela só muda pelo fluxo de recuperação.
@DocumentoCompativel
public record VeterinarioUpdateRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 dígitos (CPF) ou 14 dígitos (CNPJ), sem pontos nem traço") String cpfCnpj,
        @NotNull TipoPessoa tipoPessoa,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Celular String telefone,
        @NotBlank @Pattern(regexp = "\\d{1,5}", message = "deve ter de 1 a 5 dígitos numéricos") String crmv,
        @NotNull UnidadeFederativa crmvUf,
        Especializacao especializacao
) implements DadosDocumento {
}
