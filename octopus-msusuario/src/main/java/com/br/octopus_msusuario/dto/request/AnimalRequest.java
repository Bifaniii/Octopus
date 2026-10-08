package com.br.octopus_msusuario.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.br.octopus_msusuario.domain.Animal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// O tutor vem da rota (/api/tutores/{id}/animais) ou do TutorRequest que contém o animal.
public record AnimalRequest(
        @NotBlank @Size(max = 100)
        String nome,
       
        @NotBlank(message = "Especie é obrigatório")
        @Size(max = 250)
        String especie,
       
        @PastOrPresent
        LocalDate dataAntirrabica,

        // Opcional: id de um animal já cadastrado.
        UUID maeId
) {
    public Animal toEntity(Animal mae) {
        return Animal.builder()
                .nome(nome)
                .especie(especie)
                .dataUltimaAntirrabica(dataAntirrabica)
                .mae(mae)
                .build();
    }
}
