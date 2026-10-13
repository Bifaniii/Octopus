package com.br.octopus_msdoses.dto.response;

import com.br.octopus_msdoses.domain.ItemSeNecessario;

import java.time.LocalDateTime;

// liberadoEm: a partir de quando a próxima aplicação é aceita (null se nunca foi aplicado).
public record ItemSeNecessarioResponse(
        Long itemPrescricaoId,
        Long prescricaoId,
        Long internacaoId,
        Long medicacaoId,
        int intervaloMinimoHoras,
        boolean ativo,
        LocalDateTime ultimaAplicacao,
        String ultimaAplicacaoPor,
        LocalDateTime liberadoEm,
        boolean liberado
) {
    public static ItemSeNecessarioResponse from(ItemSeNecessario item, LocalDateTime ultima, String por,
                                                LocalDateTime agora) {
        LocalDateTime liberadoEm = ultima == null ? null : ultima.plusHours(item.getIntervaloMinimoHoras());
        return new ItemSeNecessarioResponse(item.getItemPrescricaoId(), item.getPrescricaoId(), item.getInternacaoId(),
                item.getMedicacaoId(), item.getIntervaloMinimoHoras(), item.isAtivo(), ultima, por, liberadoEm,
                item.isAtivo() && (liberadoEm == null || !agora.isBefore(liberadoEm)));
    }
}
