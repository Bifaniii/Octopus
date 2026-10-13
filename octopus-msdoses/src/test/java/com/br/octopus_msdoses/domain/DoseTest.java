package com.br.octopus_msdoses.domain;

import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.domain.enums.TipoEsquema;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoseTest {

    private static final LocalDateTime AS_14H = LocalDateTime.of(2026, 10, 10, 14, 0);

    private Dose dose() {
        return Dose.agendar(1L, 2L, 3L, 4L, TipoEsquema.CONTINUO, AS_14H);
    }

    @Test
    void doseAgendadaNasceePendenteSemAplicacao() {
        Dose dose = dose();

        assertThat(dose.getStatus()).isEqualTo(StatusDose.PENDENTE);
        assertThat(dose.getAplicadaEm()).isNull();
        assertThat(dose.getAplicadaPor()).isNull();
    }

    @Test
    void aplicarRegistraOAuxiliarEAHora() {
        Dose dose = dose();

        dose.aplicar("paulo@vidapet.local", AS_14H.plusMinutes(20), "Sem intercorrências");

        assertThat(dose.getStatus()).isEqualTo(StatusDose.APLICADA);
        assertThat(dose.getAplicadaPor()).isEqualTo("paulo@vidapet.local");
        assertThat(dose.getAplicadaEm()).isEqualTo(AS_14H.plusMinutes(20));
        assertThat(dose.getObservacao()).isEqualTo("Sem intercorrências");
    }

    // Caso de teste 9 do enunciado: a aplicação registrada pelo auxiliar Paulo não pode ser editada.
    @Test
    void rn05_aplicacaoRegistradaNaoPodeSerAlterada() {
        Dose dose = dose();
        dose.aplicar("paulo@vidapet.local", AS_14H, null);

        assertThatThrownBy(() -> dose.aplicar("outro@vidapet.local", AS_14H.plusMinutes(5), "corrigindo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-05")
                .hasMessageContaining("paulo@vidapet.local");
        assertThat(dose.getAplicadaPor()).isEqualTo("paulo@vidapet.local");
        assertThat(dose.getAplicadaEm()).isEqualTo(AS_14H);
    }

    @Test
    void cancelarSoMudaDosePendente() {
        Dose pendente = dose();
        Dose aplicada = dose();
        aplicada.aplicar("paulo@vidapet.local", AS_14H, null);

        assertThat(pendente.cancelar()).isTrue();
        assertThat(pendente.getStatus()).isEqualTo(StatusDose.CANCELADA);
        assertThat(aplicada.cancelar()).isFalse();
        assertThat(aplicada.getStatus()).isEqualTo(StatusDose.APLICADA);
    }

    @Test
    void doseCanceladaNaoPodeSerAplicada() {
        Dose dose = dose();
        dose.cancelar();

        assertThatThrownBy(() -> dose.aplicar("paulo@vidapet.local", AS_14H, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CANCELADA");
    }

    // Caso de teste 5 do enunciado e os dois limites da janela, que são inclusivos.
    @Test
    void rn03_aceitaDentroDaJanelaInclusiveNosLimites() {
        Dose antes = dose();
        Dose depois = dose();
        Dose caso5 = dose();

        antes.aplicar("paulo@vidapet.local", AS_14H.minusMinutes(30), null);
        depois.aplicar("paulo@vidapet.local", AS_14H.plusMinutes(30), null);
        caso5.aplicar("paulo@vidapet.local", AS_14H.plusMinutes(20), null);

        assertThat(caso5.getStatus()).isEqualTo(StatusDose.APLICADA);
    }

    @Test
    void rn03_recusaForaDaJanela() {
        Dose cedo = dose();
        Dose tarde = dose();

        assertThatThrownBy(() -> cedo.aplicar("paulo@vidapet.local", AS_14H.minusMinutes(31), null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-03")
                .hasMessageContaining("13:30")
                .hasMessageContaining("14:30");
        assertThatThrownBy(() -> tarde.aplicar("paulo@vidapet.local", AS_14H.plusMinutes(31), null))
                .hasMessageContaining("RN-03");
        assertThat(cedo.getStatus()).isEqualTo(StatusDose.PENDENTE);
    }

    @Test
    void rn04_janelaSoFechaDepoisDosTrintaMinutos() {
        Dose dose = dose();

        assertThat(dose.passouDaJanela(AS_14H.plusMinutes(30))).isFalse();
        assertThat(dose.passouDaJanela(AS_14H.plusMinutes(31))).isTrue();
    }

    @Test
    void rn04_dosePerdidaNaoPodeSerAplicada() {
        Dose dose = dose();
        dose.marcarPerdida();

        assertThatThrownBy(() -> dose.aplicar("paulo@vidapet.local", AS_14H, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-04");
    }
}
