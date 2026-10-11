package com.br.octopus_msdoses.dto.response;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;

import java.time.LocalDateTime;

public record DoseResponse(
        Long id,
        Long prescricaoId,
        Long itemPrescricaoId,
        Long internacaoId,
        Long medicacaoId,
        LocalDateTime horarioPrevisto,
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
                dose.getHorarioPrevisto(),
                dose.getStatus(),
                dose.getAplicadaEm(),
                dose.getAplicadaPor(),
                dose.getObservacao()
        );
    }
}
