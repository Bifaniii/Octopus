package com.br.octopus_msusuario.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TutorRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 255) String endereco,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Size(max = 20) String telefone,
        @Valid List<AnimalRequest> animais
) {
}
