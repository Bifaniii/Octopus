package com.br.octopus_medications.domain.dto.response;

import java.util.LocalDateTime;

public record MedicacaoResponse(
    Long id,
    String nomeComercial,
    String principioAtivo,
    String concentracao,
    String formaFarmaceutica,
    String unidadeMedidaEmbalagem,
    LocalDateTime dataVencimento,
    String numeroRegistroAnvisa
) {
    public static MedicacaoResponse from(Medicacao medicacao) {
        return new MedicacaoResponse(
            medicacao.getId(),
            medicacao.getNomeComercial(),
            medicacao.getPrincipioAtivo(),
            medicacao.getConcentracao(),
            medicacao.getFormaFarmaceutica(),
            medicacao.getUnidadeMedidaEmbalagem(),
            medicacao.getDataVencimento(),
            medicacao.getNumeroRegistroAnvisa(),
        );
    }

}
