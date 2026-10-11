package com.br.octopus_msinternacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record AdmissaoRequest(
        @NotNull Long animalId,
        @NotNull Long baiaId,
        @NotBlank @Size(max = 500) String motivo
) {
}
