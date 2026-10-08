package com.br.octopus_msinternacao.dto.response;

import com.br.octopus_msinternacao.domain.InternacaoEvento;
import com.br.octopus_msinternacao.domain.enums.StatusInternacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record InternacaoEventoResponse(
        StatusInternacao statusAnterior,
        StatusInternacao statusNovo,
        UUID baiaId,
        String usuario,
        LocalDateTime dataHora
) {
    public static InternacaoEventoResponse from(InternacaoEvento evento) {
        return new InternacaoEventoResponse(
                evento.getStatusAnterior(),
                evento.getStatusNovo(),
                evento.getBaiaId(),
                evento.getUsuario(),
                evento.getDataHora()
        );
    }
}
