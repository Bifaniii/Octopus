package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Tutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TutorResponse(
        UUID id,
        String nome,
        String endereco,
        LocalDate dataNascimento,
        String telefone,
        List<AnimalResponse> animais
) {
    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getNome(),
                tutor.getEndereco(),
                tutor.getDataNascimento(),
                tutor.getTelefone(),
                tutor.getAnimais().stream().map(AnimalResponse::from).toList()
        );
    }
}
