package com.br.octopus_msprescricao.client.dto;

import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Corpo de POST /api/doses/agendamentos no octopus-msdoses: uma dose por horário gerado em cada item com horário
// fixo; o item "se necessário" vai sem horários e com o intervalo mínimo.
public record AgendamentoDosesDto(Long prescricaoId, Long internacaoId, List<Item> itens) {

    public record Item(Long itemPrescricaoId, Long medicacaoId, String esquema, boolean seNecessario,
                       Integer intervaloMinimoHoras, List<LocalDateTime> horarios) {
    }

    /** {@code medicacoes}: o medicamento de cada item, já buscado no msmedications, de onde vem o esquema. */
    public static AgendamentoDosesDto de(Prescricao prescricao, Map<Long, MedicacaoDto> medicacoes) {
        return new AgendamentoDosesDto(
                prescricao.getId(),
                prescricao.getInternacaoId(),
                prescricao.getItens().stream().map(item -> item(item, medicacoes)).toList()
        );
    }

    private static Item item(ItemPrescricao item, Map<Long, MedicacaoDto> medicacoes) {
        String esquema = medicacoes.get(item.getMedicacaoId()).tipoEsquema();
        return new Item(item.getId(), item.getMedicacaoId(), esquema, item.isSeNecessario(),
                item.isSeNecessario() ? item.getIntervaloHoras() : null, item.horarios());
    }
}
