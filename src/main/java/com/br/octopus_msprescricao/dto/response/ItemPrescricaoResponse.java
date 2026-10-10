package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.ItemPrescricao;

import java.time.LocalDateTime;


public record ItemPrescricaoResponse(
        Long id,
        Long medicacaoId,
        int intervaloHoras,
        LocalDateTime inicio,
        String observacao
) {
    public static ItemPrescricaoResponse from(ItemPrescricao item) {
        return new ItemPrescricaoResponse(
                item.getId(),
                item.getMedicacaoId(),
                item.getIntervaloHoras(),
                item.getInicio(),
                item.getObservacao()
        );
    }
}
