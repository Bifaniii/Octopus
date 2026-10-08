package com.br.octopus_msprescricao.dto.response;

import com.br.octopus_msprescricao.domain.Dose;
import com.br.octopus_msprescricao.domain.enums.StatusDose;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoseResponse(
        UUID id,
        LocalDateTime horarioPrevisto,
        StatusDose status
) {
    public static DoseResponse from(Dose dose) {
        return new DoseResponse(dose.getId(), dose.getHorarioPrevisto(), dose.getStatus());
    }
}
