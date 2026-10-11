package com.br.octopus_msdoses.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

// Enviado pelo octopus-msprescricao ao gravar uma prescrição: os horários já vêm gerados de lá.
public record AgendamentoRequest(
        @NotNull(message = "A prescrição é obrigatória")
        Long prescricaoId,

        @NotNull(message = "A internação é obrigatória")
        Long internacaoId,

        @NotEmpty(message = "O agendamento precisa de ao menos um item")
        List<@Valid Item> itens
) {
    public record Item(
            @NotNull(message = "O item da prescrição é obrigatório")
            Long itemPrescricaoId,

            @NotNull(message = "O medicamento é obrigatório")
            Long medicacaoId,

            @NotEmpty(message = "O item precisa de ao menos um horário")
            List<@NotNull LocalDateTime> horarios
    ) {
    }
}
