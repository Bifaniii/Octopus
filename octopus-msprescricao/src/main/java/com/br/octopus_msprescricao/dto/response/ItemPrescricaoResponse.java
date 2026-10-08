package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.ItemPrescricao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ItemPrescricaoResponse(
        UUID id,
        UUID medicacaoId,
        String dosagem,
        int intervaloHoras,
        int quantidadeDoses,
        LocalDateTime inicio,
        String observacao,
        List<DoseResponse> doses
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
                item.getDoses().stream().map(DoseResponse::from).toList()
        );
    }
}
