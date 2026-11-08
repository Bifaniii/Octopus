package com.br.octopus_msusuario.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaxIdadeValidatorTest {

    private final LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private MaxIdadeValidator com(int maximo) {
        MaxIdade anotacao = mock(MaxIdade.class);
        when(anotacao.value()).thenReturn(maximo);
        MaxIdadeValidator validador = new MaxIdadeValidator();
        validador.initialize(anotacao);
        return validador;
    }

    @Test
    void setentaAnosAindaValemEOAniversarioDeSetentaEUmJaNao() {
        MaxIdadeValidator v = com(70);

        assertThat(v.isValid(hoje.minusYears(70), null)).isTrue();
        assertThat(v.isValid(hoje.minusYears(71).plusDays(1), null)).isTrue(); // falta um dia para os 71
        assertThat(v.isValid(hoje.minusYears(71), null)).isFalse();             // fez 71 hoje
        assertThat(v.isValid(hoje.minusYears(200), null)).isFalse();
        assertThat(v.isValid(hoje.minusYears(30), null)).isTrue();
    }

    @Test
    void dataNulaOuFuturaFicaComOutrasAnotacoes() {
        MaxIdadeValidator v = com(70);

        assertThat(v.isValid(null, null)).isTrue();
        assertThat(v.isValid(hoje.plusDays(1), null)).isTrue(); // quem barra é o @Past
    }
}
