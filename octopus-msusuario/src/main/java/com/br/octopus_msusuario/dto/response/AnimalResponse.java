package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Animal;

import java.time.LocalDate;
import java.util.UUID;

public record AnimalResponse(
        UUID id,
        String nome,
        UUID tutorId,
        String especie,
        LocalDate dataUltimaAntirrabica,
        UUID maeId
) {
    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getNome(),
                animal.getTutor().getId(),
                animal.getEspecie(),
                animal.getDataUltimaAntirrabica(),
                animal.getMae() != null ? animal.getMae().getId() : null
        );
    }
}
