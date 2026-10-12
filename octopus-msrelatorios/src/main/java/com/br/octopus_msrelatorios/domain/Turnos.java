package com.br.octopus_msrelatorios.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Horários de início dos turnos da clínica. O enunciado pede o relatório "por turno" sem dizer os horários; em 12/10
 * ficou definido manhã das 6h às 12h, tarde das 12h às 18h e noite das 18h às 6h, ajustáveis em app.relatorios.turnos.*.
 */
@Component
public class Turnos {

    private final LocalTime manha;
    private final LocalTime tarde;
    private final LocalTime noite;

    public Turnos(@Value("${app.relatorios.turnos.manha:06:00}") LocalTime manha,
                  @Value("${app.relatorios.turnos.tarde:12:00}") LocalTime tarde,
                  @Value("${app.relatorios.turnos.noite:18:00}") LocalTime noite) {
        if (!manha.isBefore(tarde) || !tarde.isBefore(noite)) {
            throw new IllegalStateException("Os turnos precisam estar em ordem: manhã < tarde < noite.");
        }
        this.manha = manha;
        this.tarde = tarde;
        this.noite = noite;
    }

    public Turno de(LocalTime hora) {
        if (!hora.isBefore(manha) && hora.isBefore(tarde)) {
            return Turno.MANHA;
        }
        if (!hora.isBefore(tarde) && hora.isBefore(noite)) {
            return Turno.TARDE;
        }
        return Turno.NOITE;
    }

    public String descricao(Turno turno) {
        return switch (turno) {
            case MANHA -> turno.rotulo() + " (" + manha + " às " + tarde + ")";
            case TARDE -> turno.rotulo() + " (" + tarde + " às " + noite + ")";
            case NOITE -> turno.rotulo() + " (" + noite + " às " + manha + ")";
        };
    }
}
