package com.br.octopus_msmedications.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Quantidade de doses reservadas ou devolvidas de uma vez (1 dose = 1 unidade).
public record MovimentoEstoqueRequest(
        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade
) {
}
