package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDate;
import java.util.List;

public record TutorDto(Long id, String nome, String endereco, LocalDate dataNascimento, String telefone,
                       String email, List<AnimalDto> animais) {

    public record AnimalDto(Long id, String nome, Long tutorId, String especie, LocalDate dataUltimaAntirrabica,
                            Long maeId) {
    }
}
