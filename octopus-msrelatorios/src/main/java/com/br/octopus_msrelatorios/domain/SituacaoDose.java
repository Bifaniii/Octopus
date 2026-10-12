package com.br.octopus_msrelatorios.domain;

import com.br.octopus_msrelatorios.client.dto.DoseDto;

import java.time.LocalDateTime;

/**
 * Situação da dose no momento do relatório. O msdoses só guarda PENDENTE, APLICADA e CANCELADA; atrasada e perdida
 * saem da janela de 30 minutos (RN-03 e RN-04), calculadas aqui na consulta, sem job agendado.
 */
public enum SituacaoDose {

    APLICADA("Aplicada"),
    ATRASADA("Atrasada"),
    PERDIDA("Perdida"),
    PREVISTA("Prevista"),
    CANCELADA("Cancelada");

    public static final int JANELA_MINUTOS = 30;

    private final String rotulo;

    SituacaoDose(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static SituacaoDose de(DoseDto dose, LocalDateTime agora) {
        return switch (dose.status()) {
            case "APLICADA" -> APLICADA;
            case "CANCELADA" -> CANCELADA;
            default -> {
                if (agora.isBefore(dose.horarioPrevisto())) {
                    yield PREVISTA;
                }
                yield agora.isAfter(dose.horarioPrevisto().plusMinutes(JANELA_MINUTOS)) ? PERDIDA : ATRASADA;
            }
        };
    }
}
