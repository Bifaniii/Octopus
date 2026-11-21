package com.br.octopus_msusuario.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

// Data da aplicação da vacina antirrábica, enviada pelo octopus-msdoses quando o auxiliar aplica a dose.
public record AntirrabicaRequest(
        @NotNull(message = "Data é obrigatória")
        @PastOrPresent(message = "A data da antirrábica não pode ser futura")
        LocalDate data
) {
}
