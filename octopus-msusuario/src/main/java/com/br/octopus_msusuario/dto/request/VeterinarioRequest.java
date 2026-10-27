package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.validation.NomeCompleto;
import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import com.br.octopus_msusuario.domain.enums.UnidadeFederativa;
import com.br.octopus_msusuario.validation.Documentos;
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

@DocumentoCompativel
public record VeterinarioRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String senha,
        @NotBlank @Size(max = 100) @NomeCompleto String nome,
        @NotBlank @Pattern(regexp = "\\d{11}|" + Documentos.CNPJ_REGEX, message = "deve conter 11 dígitos (CPF) ou 14 caracteres (CNPJ, com números e letras maiúsculas), sem pontos nem traço") String cpfCnpj,
        @NotNull TipoPessoa tipoPessoa,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Celular String telefone,
        @NotBlank @Pattern(regexp = "\\d{1,5}", message = "deve ter de 1 a 5 dígitos numéricos") String crmv,
        @NotNull UnidadeFederativa crmvUf,
        Especializacao especializacao
) implements DadosDocumento {
}
