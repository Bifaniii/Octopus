package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Recepcionista;

import java.time.LocalDate;
import java.util.UUID;

public record RecepcionistaResponse(
        UUID id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String telefone,
        UsuarioResponse usuario
) {
    public static RecepcionistaResponse from(Recepcionista recepcionista) {
        return new RecepcionistaResponse(
                recepcionista.getId(),
                recepcionista.getNome(),
                recepcionista.getCpf(),
                recepcionista.getDataNascimento(),
                recepcionista.getTelefone(),
                UsuarioResponse.from(recepcionista.getUsuario())
        );
    }
}
