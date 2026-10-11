package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.ItemPrescricao;

import java.time.LocalDateTime;
import java.util.List;

public record ItemPrescricaoResponse(
        Long id,
        Long medicacaoId,
        String dosagem,
        int intervaloHoras,
        int quantidadeDoses,
        LocalDateTime inicio,
        String observacao,
        List<LocalDateTime> horarios
) {
    public static ItemPrescricaoResponse from(ItemPrescricao item) {
        return new ItemPrescricaoResponse(
                item.getId(),
                item.getMedicacaoId(),
                item.getDosagem(),
                item.getIntervaloHoras(),
                item.getQuantidadeDoses(),
                item.getInicio(),
                item.getObservacao(),
                item.horarios()
        );
    }
}
