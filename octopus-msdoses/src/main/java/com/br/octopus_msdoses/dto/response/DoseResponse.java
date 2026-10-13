package com.br.octopus_msdoses.dto.response;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.domain.enums.TipoEsquema;

import java.time.LocalDateTime;

public record DoseResponse(
        Long id,
        Long prescricaoId,
        Long itemPrescricaoId,
        Long internacaoId,
        Long medicacaoId,
        TipoEsquema esquema,
        boolean seNecessario,
        Long reaplicacaoDe,
        LocalDateTime horarioPrevisto,
        LocalDateTime janelaInicio,
        LocalDateTime janelaFim,
        StatusDose status,
        LocalDateTime aplicadaEm,
        String aplicadaPor,
        String observacao
) {
    public static DoseResponse from(Dose dose) {
        return new DoseResponse(
                dose.getId(),
                dose.getPrescricaoId(),
                dose.getItemPrescricaoId(),
                dose.getInternacaoId(),
                dose.getMedicacaoId(),
                dose.getEsquema(),
                dose.isSeNecessario(),
                dose.getReaplicacaoDe(),
                dose.getHorarioPrevisto(),
                dose.isSeNecessario() ? null : dose.inicioDaJanela(),
                dose.isSeNecessario() ? null : dose.fimDaJanela(),
                dose.getStatus(),
                dose.getAplicadaEm(),
                dose.getAplicadaPor(),
                dose.getObservacao()
        );
    }
}
