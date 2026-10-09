package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.Prescricao;

import java.time.LocalDateTime;
import java.util.List;

public record PrescricaoResponse(
        Long id,
        Long internacaoId,
        String veterinarioEmail,
        String observacao,
        boolean ativo,
        LocalDateTime criadoEm,
        List<ItemPrescricaoResponse> itens
) {
    // Percorre a coleção LAZY de itens: chame dentro de uma transação (open-in-view está desligado) ou com a
    // entidade recém-salva, que já tem os itens em memória.
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
