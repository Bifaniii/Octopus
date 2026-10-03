package com.br.octopus_msbaias.dto.request;

import com.br.octopus_msbaias.domain.enums.Tipo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BaiaRequest(
        @NotNull Tipo tipo,
        @NotBlank @Size(max = 25) String nome,
        @Size(max = 255) String descricao,
        @NotNull @Min(1) Integer capacidade
) {
}
