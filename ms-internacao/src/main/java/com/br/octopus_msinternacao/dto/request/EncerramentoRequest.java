package com.br.octopus_msinternacao.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// RN-08: momento em que o animal saiu fisicamente da baia. A coerência com a alta e com o relógio da clínica
// é conferida na entidade, que usa o Clock; @PastOrPresent usaria o fuso do servidor (UTC).
public record EncerramentoRequest(@NotNull LocalDateTime dataSaida) {
}
