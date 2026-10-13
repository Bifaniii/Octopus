package com.br.octopus_msdoses.dto.request;

import com.br.octopus_msdoses.domain.enums.TipoEsquema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

// Enviado pelo octopus-msprescricao ao gravar uma prescrição: os horários já vêm gerados de lá. Item "se
// necessário" vem sem horários e com o intervalo mínimo entre aplicações (conferido no DoseService).
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

            @NotNull(message = "O esquema do medicamento é obrigatório")
            TipoEsquema esquema,

            // Ausente vale como false: o msprescricao só manda quando o item é "se necessário".
            Boolean seNecessario,

            @Min(value = 1, message = "O intervalo mínimo é de 1 hora")
            @Max(value = 168, message = "O intervalo máximo é de 168 horas")
            Integer intervaloMinimoHoras,

            List<@NotNull LocalDateTime> horarios
    ) {
        public boolean ehSeNecessario() {
            return Boolean.TRUE.equals(seNecessario);
        }
    }
}
