package com.br.octopus_msprescricao.dto.request;

import com.br.octopus_msprescricao.domain.ItemPrescricao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;


// O teto de 168 h (1 semana) evita que um valor absurdo passe. Ajuste se a clínica precisar.
public record ItemPrescricaoRequest(
        @NotNull(message = "O medicamento é obrigatório")
        Long medicacaoId,

        @NotNull(message = "O intervalo é obrigatório")
        @Min(value = 1, message = "O intervalo mínimo é de 1 hora")
        @Max(value = 168, message = "O intervalo máximo é de 168 horas")
        Integer intervaloHoras,

        @NotNull(message = "O horário de início é obrigatório")
        LocalDateTime inicio,

        @Size(max = 500)
        String observacao
) {
    public ItemPrescricao toEntity() {
        return ItemPrescricao.builder()
                .medicacaoId(medicacaoId)
                .intervaloHoras(intervaloHoras)
                .inicio(inicio)
                .observacao(observacao)
                .build();
    }
}
