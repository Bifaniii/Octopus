package com.br.octopus_msrelatorios.domain;

import com.br.octopus_msrelatorios.client.dto.DoseDto;

import java.time.LocalDateTime;

/**
 * Situação da dose no momento do relatório. A perdida vem marcada do msdoses (RN-04), que varre as doses antes de
 * responder; a atrasada (pendente, passou do horário, ainda na janela de 30 minutos da RN-03) é calculada aqui. O
 * cálculo da perdida continua como reserva, para uma pendente que já tenha passado da janela.
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
            case "PERDIDA" -> PERDIDA;
            default -> {
                if (agora.isBefore(dose.horarioPrevisto())) {
                    yield PREVISTA;
                }
                yield agora.isAfter(dose.horarioPrevisto().plusMinutes(JANELA_MINUTOS)) ? PERDIDA : ATRASADA;
            }
        };
    }
}
