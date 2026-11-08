package com.br.octopus_msusuario.dto.request;

import com.br.octopus_msusuario.validation.MaxIdade;
import com.br.octopus_msusuario.validation.MinIdade;
import com.br.octopus_msusuario.validation.NomeCompleto;
import com.br.octopus_msusuario.validation.Celular;
import com.br.octopus_msusuario.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecepcionistaRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String senha,
        @NotBlank @Size(max = 100) @NomeCompleto String nome,
        @NotBlank @Cpf String cpf,
        @NotNull @Past @MinIdade(value = MinIdade.ADULTO, message = "A pessoa precisa ter pelo menos {value} anos") @MaxIdade(value = MaxIdade.FUNCIONARIO, message = "A pessoa não pode ter mais de {value} anos") LocalDate dataNascimento,
        @NotBlank @Celular String telefone
) {
}
