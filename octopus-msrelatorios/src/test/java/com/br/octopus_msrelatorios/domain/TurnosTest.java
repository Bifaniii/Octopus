package com.br.octopus_msrelatorios.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnosTest {

    private final Turnos turnos = new Turnos(LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0));

    @Test
    void cadaHorarioCaiNoSeuTurno() {
        assertThat(turnos.de(LocalTime.of(6, 0))).isEqualTo(Turno.MANHA);
        assertThat(turnos.de(LocalTime.of(11, 59))).isEqualTo(Turno.MANHA);
        assertThat(turnos.de(LocalTime.of(12, 0))).isEqualTo(Turno.TARDE);
        assertThat(turnos.de(LocalTime.of(18, 0))).isEqualTo(Turno.NOITE);
        assertThat(turnos.de(LocalTime.of(0, 30))).isEqualTo(Turno.NOITE);
        assertThat(turnos.de(LocalTime.of(5, 59))).isEqualTo(Turno.NOITE);
    }

    @Test
    void descricaoMostraOsHorarios() {
        assertThat(turnos.descricao(Turno.NOITE)).isEqualTo("Noite (18:00 às 06:00)");
    }

    @Test
    void turnosForaDeOrdemNaoSobem() {
        assertThatThrownBy(() -> new Turnos(LocalTime.of(12, 0), LocalTime.of(6, 0), LocalTime.of(18, 0)))
                .isInstanceOf(IllegalStateException.class);
    }
}
