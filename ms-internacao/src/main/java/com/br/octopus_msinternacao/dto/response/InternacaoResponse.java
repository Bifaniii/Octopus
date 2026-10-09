package com.br.octopus_msinternacao.dto.response;

import com.br.octopus_msinternacao.domain.Internacao;
import com.br.octopus_msinternacao.domain.enums.StatusInternacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record InternacaoResponse(
        Long id,
        UUID animalId,
        String animalNome,
        String animalEspecie,
        UUID baiaId,
        UUID maeId,
        StatusInternacao status,
        String motivo,
        String termoResponsabilidade,
        LocalDateTime dataAdmissao,
        LocalDateTime dataAlta,
        LocalDateTime dataSaida,
        String registradoPor
) {
    public static InternacaoResponse from(Internacao internacao) {
        return new InternacaoResponse(
                internacao.getId(),
                internacao.getAnimalId(),
                internacao.getAnimalNome(),
                internacao.getAnimalEspecie(),
                internacao.getBaiaId(),
                internacao.getMaeId(),
                internacao.getStatus(),
                internacao.getMotivo(),
                internacao.getTermoResponsabilidade(),
                internacao.getDataAdmissao(),
                internacao.getDataAlta(),
                internacao.getDataSaida(),
                internacao.getRegistradoPor()
        );
    }
}
