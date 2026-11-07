package com.br.octopus_msusuario.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinIdadeValidatorTest {

    private final LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private MinIdadeValidator com(int minimo) {
        MinIdade anotacao = mock(MinIdade.class);
        when(anotacao.value()).thenReturn(minimo);
        MinIdadeValidator validador = new MinIdadeValidator();
        validador.initialize(anotacao);
        return validador;
    }

    @Test
    void contaOsAnosCompletosEOAniversarioValeNoProprioDia() {
        MinIdadeValidator v = com(23);

        assertThat(v.isValid(hoje.minusYears(23), null)).isTrue();
        assertThat(v.isValid(hoje.minusYears(40), null)).isTrue();
        assertThat(v.isValid(hoje.minusYears(23).plusDays(1), null)).isFalse();
        assertThat(v.isValid(hoje.minusYears(22), null)).isFalse();
    }

    @Test
    void nascimentoDe29DeFevereiroNaoQuebraAContaEDataNulaOuFuturaFicaComOutrasAnotacoes() {
        MinIdadeValidator v = com(23);

        assertThat(v.isValid(LocalDate.of(2000, 2, 29), null)).isTrue();
        assertThat(v.isValid(null, null)).isTrue();
        assertThat(v.isValid(hoje.plusDays(1), null)).isTrue(); // quem barra é o @Past
    }
}
