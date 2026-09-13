package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Auxiliar;

import java.time.LocalDate;
import java.util.UUID;

public record AuxiliarResponse(
        UUID id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String telefone,
        UsuarioResponse usuario
) {
    public static AuxiliarResponse from(Auxiliar auxiliar) {
        return new AuxiliarResponse(
                auxiliar.getId(),
                auxiliar.getNome(),
                auxiliar.getCpf(),
                auxiliar.getDataNascimento(),
                auxiliar.getTelefone(),
                UsuarioResponse.from(auxiliar.getUsuario())
        );
    }
}
