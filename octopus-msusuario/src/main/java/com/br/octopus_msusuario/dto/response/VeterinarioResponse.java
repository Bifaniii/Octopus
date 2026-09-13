package com.br.octopus_msusuario.dto.response;

import com.br.octopus_msusuario.domain.Veterinario;
import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;

import java.time.LocalDate;
import java.util.UUID;

public record VeterinarioResponse(
        UUID id,
        String nome,
        String cpfCnpj,
        TipoPessoa tipoPessoa,
        LocalDate dataNascimento,
        String telefone,
        String crmv,
        String crmvUf,
        Especializacao especializacao,
        UsuarioResponse usuario
) {
    public static VeterinarioResponse from(Veterinario veterinario) {
        return new VeterinarioResponse(
                veterinario.getId(),
                veterinario.getNome(),
                veterinario.getCpfCnpj(),
                veterinario.getTipoPessoa(),
                veterinario.getDataNascimento(),
                veterinario.getTelefone(),
                veterinario.getCrmv(),
                veterinario.getCrmvUf(),
                veterinario.getEspecializacao(),
                UsuarioResponse.from(veterinario.getUsuario())
        );
    }
}
