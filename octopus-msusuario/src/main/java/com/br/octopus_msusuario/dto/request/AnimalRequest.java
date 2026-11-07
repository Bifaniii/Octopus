package com.br.octopus_msusuario.dto.request;

import java.time.LocalDate;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.enums.Especie;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// O tutor vem da rota (/api/tutores/{id}/animais) ou do TutorRequest que contém o animal.
public record AnimalRequest(
        @NotBlank @Size(max = 100)
        String nome,
       
        // "Cão", "Gato" ou "Outro"; outro valor recebe 400 com a lista dos aceitos.
        @NotNull(message = "Espécie é obrigatória")
        Especie especie,

        @NotNull(message = "Sexo é obrigatório")
        SexoAnimal sexo,
       
        @PastOrPresent
        LocalDate dataAntirrabica,

        // Opcional: id de um animal já cadastrado.
        Long maeId
) {
    public Animal toEntity(Animal mae) {
        return Animal.builder()
                .nome(nome)
                .especie(especie)
                .sexo(sexo)
                .dataUltimaAntirrabica(dataAntirrabica)
                .mae(mae)
                .build();
    }
}
