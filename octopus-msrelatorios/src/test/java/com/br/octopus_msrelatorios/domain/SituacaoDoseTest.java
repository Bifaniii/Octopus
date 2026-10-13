package com.br.octopus_msrelatorios.domain;

import com.br.octopus_msrelatorios.client.dto.DoseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SituacaoDoseTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 12, 8, 0);

    private static DoseDto dose(String status) {
        return new DoseDto(1L, 1L, 1L, 1L, HORARIO, status, null, null, null, null, null);
    }

    @Test
    void pendenteAntesDoHorarioEstaPrevista() {
        assertThat(SituacaoDose.de(dose("PENDENTE"), HORARIO.minusMinutes(1))).isEqualTo(SituacaoDose.PREVISTA);
    }

    @Test
    void pendenteAteTrintaMinutosDepoisEstaAtrasada() {
        assertThat(SituacaoDose.de(dose("PENDENTE"), HORARIO)).isEqualTo(SituacaoDose.ATRASADA);
        assertThat(SituacaoDose.de(dose("PENDENTE"), HORARIO.plusMinutes(30))).isEqualTo(SituacaoDose.ATRASADA);
    }

    @Test
    void pendenteForaDaJanelaEstaPerdida() {
        assertThat(SituacaoDose.de(dose("PENDENTE"), HORARIO.plusMinutes(31))).isEqualTo(SituacaoDose.PERDIDA);
    }

    @Test
    void aplicadaECanceladaNaoDependemDaHora() {
        assertThat(SituacaoDose.de(dose("APLICADA"), HORARIO.plusDays(1))).isEqualTo(SituacaoDose.APLICADA);
        assertThat(SituacaoDose.de(dose("CANCELADA"), HORARIO.minusDays(1))).isEqualTo(SituacaoDose.CANCELADA);
    }

    // RN-04: o msdoses já marca a perdida; vale mesmo que o relatório seja gerado dentro da janela de outra hora.
    @Test
    void perdidaMarcadaNoMsdosesEPerdida() {
        assertThat(SituacaoDose.de(dose("PERDIDA"), HORARIO.plusMinutes(10))).isEqualTo(SituacaoDose.PERDIDA);
    }

    @Test
    void observacaoIdentificaReaplicacaoESeNecessario() {
        DoseDto reaplicacao = new DoseDto(2L, 1L, 1L, 1L, HORARIO, "APLICADA", HORARIO, "a@b", "ok", 1L, false);
        DoseDto seNecessario = new DoseDto(3L, 1L, 1L, 1L, HORARIO, "APLICADA", HORARIO, "a@b", null, null, true);

        assertThat(reaplicacao.observacaoDoRelatorio()).isEqualTo("Reaplicação (RN-07). ok");
        assertThat(seNecessario.observacaoDoRelatorio()).isEqualTo("Se necessário");
        assertThat(dose("APLICADA").observacaoDoRelatorio()).isNull();
    }
}
