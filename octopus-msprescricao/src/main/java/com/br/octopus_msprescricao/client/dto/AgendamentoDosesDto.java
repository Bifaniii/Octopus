package com.br.octopus_msprescricao.client.dto;

import com.br.octopus_msprescricao.domain.Prescricao;

import java.time.LocalDateTime;
import java.util.List;

// Corpo de POST /api/doses/agendamentos no octopus-msdoses: uma dose por horário gerado em cada item.
public record AgendamentoDosesDto(Long prescricaoId, Long internacaoId, List<Item> itens) {

    public record Item(Long itemPrescricaoId, Long medicacaoId, List<LocalDateTime> horarios) {
    }

    public static AgendamentoDosesDto de(Prescricao prescricao) {
        return new AgendamentoDosesDto(
                prescricao.getId(),
                prescricao.getInternacaoId(),
                prescricao.getItens().stream()
                        .map(item -> new Item(item.getId(), item.getMedicacaoId(), item.horarios()))
                        .toList()
        );
    }
}
