package com.br.octopus_msprescricao.dto.request;

import com.br.octopus_msprescricao.domain.ItemPrescricao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Os tetos (168 h = 1 semana; 200 doses) evitam que um valor absurdo gere milhares de linhas. Ajuste se a clínica precisar.
public record ItemPrescricaoRequest(
        @NotNull(message = "O medicamento é obrigatório")
        Long medicacaoId,

        @NotBlank(message = "A dosagem é obrigatória")
        @Size(max = 100)
        String dosagem,

        @NotNull(message = "O intervalo é obrigatório")
        @Min(value = 1, message = "O intervalo mínimo é de 1 hora")
        @Max(value = 168, message = "O intervalo máximo é de 168 horas")
        Integer intervaloHoras,

        @NotNull(message = "A quantidade de doses é obrigatória")
        @Min(value = 1, message = "Informe ao menos 1 dose")
        @Max(value = 200, message = "O máximo é de 200 doses por item")
        Integer quantidadeDoses,

        @NotNull(message = "O horário da primeira dose é obrigatório")
        LocalDateTime inicio,

        @Size(max = 500)
        String observacao
) {
    public ItemPrescricao toEntity() {
        return ItemPrescricao.builder()
                .medicacaoId(medicacaoId)
                .dosagem(dosagem)
                .intervaloHoras(intervaloHoras)
                .quantidadeDoses(quantidadeDoses)
                .inicio(inicio)
                .observacao(observacao)
                .build();
    }
}
