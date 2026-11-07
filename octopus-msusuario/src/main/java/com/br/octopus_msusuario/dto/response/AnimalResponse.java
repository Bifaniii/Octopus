package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.enums.Especie;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;

import java.time.LocalDate;

public record AnimalResponse(
        Long id,
        String nome,
        Long tutorId,
        Especie especie,
        SexoAnimal sexo,
        LocalDate dataUltimaAntirrabica,
        Long maeId
) {
    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getNome(),
                animal.getTutor().getId(),
                animal.getEspecie(),
                animal.getSexo(),
                animal.getDataUltimaAntirrabica(),
                animal.getMae() != null ? animal.getMae().getId() : null
        );
    }
}
