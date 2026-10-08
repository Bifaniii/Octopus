package com.br.octopus_msinternacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdmissaoRequest(
        @NotNull UUID animalId,
        @NotNull UUID baiaId,
        @NotBlank @Size(max = 500) String motivo
) {
}
