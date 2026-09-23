package com.br.octopus_msbaias.dto.response;

import com.br.octopus_msbaias.domain.Baia;
import com.br.octopus_msbaias.domain.enums.Tipo;

import java.util.UUID;

public record BaiaResponse(
        UUID id,
        Tipo tipo,
        String nome,
        String descricao,
        int capacidade,
        boolean ativo
) {
    public static BaiaResponse from(Baia baia) {
        return new BaiaResponse(
                baia.getId(),
                baia.getTipo(),
                baia.getNome(),
                baia.getDescricao(),
                baia.getCapacidade(),
                baia.isAtivo()
        );
    }
}
