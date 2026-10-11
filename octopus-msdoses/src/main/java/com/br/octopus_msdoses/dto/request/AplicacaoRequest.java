package com.br.octopus_msdoses.dto.request;

import jakarta.validation.constraints.Size;

// O auxiliar e a hora não vêm no corpo: são o dono do token e o relógio da clínica.
public record AplicacaoRequest(@Size(max = 500) String observacao) {
}
