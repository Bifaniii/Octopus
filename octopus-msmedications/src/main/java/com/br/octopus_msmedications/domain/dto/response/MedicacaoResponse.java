package com.br.octopus_medications.domain.dto.response;

import java.util.LocalDateTime;
import java.util.UUID;

public record MedicacaoResponse(
    UUID id,
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
