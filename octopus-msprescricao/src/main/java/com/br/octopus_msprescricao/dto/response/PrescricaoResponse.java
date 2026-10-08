package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.Prescricao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PrescricaoResponse(
        UUID id,
        UUID internacaoId,
        String veterinarioEmail,
        String observacao,
        boolean ativo,
        LocalDateTime criadoEm,
        List<ItemPrescricaoResponse> itens
) {
    // Percorre coleções LAZY: chame sempre dentro de uma transação (open-in-view está desligado).
    public static PrescricaoResponse from(Prescricao prescricao) {
        return new PrescricaoResponse(
                prescricao.getId(),
                prescricao.getInternacaoId(),
                prescricao.getVeterinarioEmail(),
                prescricao.getObservacao(),
                prescricao.isAtivo(),
                prescricao.getCriadoEm(),
                prescricao.getItens().stream().map(ItemPrescricaoResponse::from).toList()
        );
    }
}
