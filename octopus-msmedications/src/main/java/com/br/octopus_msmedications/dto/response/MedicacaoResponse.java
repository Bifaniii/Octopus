package com.br.octopus_msmedications.dto.response;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.domain.enums.TipoEsquema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MedicacaoResponse(
        UUID id,
        String nomeComercial,
        String principioAtivo,
        String concentracao,
        String formaFarmaceutica,
        String unidadeMedidaEmbalagem,
        TipoEsquema tipoEsquema,
        LocalDateTime dataVencimento,
        String fabricante,
        String numeroRegistroAnvisa,
        List<InteracaoResponse> interacoesProibidas
) {

    // Resumida, senão os dois lados do par se referenciam sem fim.
    public record InteracaoResponse(UUID id, String nomeComercial, String principioAtivo) {
    }

    public static MedicacaoResponse from(Medicacao medicacao) {
        return new MedicacaoResponse(
                medicacao.getId(),
                medicacao.getNomeComercial(),
                medicacao.getPrincipioAtivo(),
                medicacao.getConcentracao(),
                medicacao.getFormaFarmaceutica(),
                medicacao.getUnidadeMedidaEmbalagem(),
                medicacao.getTipoEsquema(),
                medicacao.getDataVencimento(),
                medicacao.getFabricante(),
                medicacao.getNumeroRegistroAnvisa(),
                medicacao.getInteracoesProibidas().stream()
                        .map(i -> new InteracaoResponse(i.getId(), i.getNomeComercial(), i.getPrincipioAtivo()))
                        .toList()
        );
    }
}
