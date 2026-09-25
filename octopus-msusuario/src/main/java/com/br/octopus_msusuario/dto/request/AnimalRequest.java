package com.br.octopus_msusuario.dto.request;

import java.time.LocalDateTime;

import com.br.octopus_msusuario.domain.Tutor;

import jakarta.validation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record AnimalRequest(
        @NotBlank @Size(max = 100) String nome,

        @NotBlank(message = "Tutor é obrigatório")
        Tutor tutor,

        @NotBlank(message = "Especie é obrigatório")
        String especie,

        LocalDateTime dataAntirrabica
) {
}
