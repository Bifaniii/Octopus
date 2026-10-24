package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Tutor;

import java.time.LocalDate;
import java.util.List;

public record TutorResponse(
        Long id,
        String nome,
        String endereco,
        LocalDate dataNascimento,
        String telefone,
        String email,
        List<AnimalResponse> animais
) {
    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getNome(),
                tutor.getEndereco(),
                tutor.getDataNascimento(),
                tutor.getTelefone(),
                tutor.getEmail(),
                tutor.getAnimais().stream().map(AnimalResponse::from).toList()
        );
    }
}
