package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Animal;

import java.util.UUID;

public record AnimalResponse(
        UUID id,
        String nome,
        UUID tutorId
) {
    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getNome(),
                animal.getTutor().getId()
        );
    }
}
