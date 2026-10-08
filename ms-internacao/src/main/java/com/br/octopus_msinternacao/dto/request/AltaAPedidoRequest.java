package com.br.octopus_msinternacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AltaAPedidoRequest(@NotBlank @Size(max = 500) String termoResponsabilidade) {
}