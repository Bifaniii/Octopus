package com.br.octopus_msinternacao.domain;

import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import com.br.octopus_msinternacao.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Máquina de estados da internação: transições do enunciado, guardas (termo, saída física) e histórico.
class InternacaoTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 8, 10, 0);
    private static final String RECEPCAO = "recepcao@vidapet.local";
    private static final String VET = "vet@vidapet.local";

    private final UUID baiaColetiva = UUID.randomUUID();

    private Internacao admitida() {
        return Internacao.admitir(UUID.randomUUID(), "Thor", "Cão", null, baiaColetiva, "Gastroenterite", RECEPCAO, T0);
    }

    private Internacao emTratamento() {
        Internacao internacao = admitida();
        internacao.iniciarTratamento(VET, T0.plusHours(1));
        return internacao;
    }

    @Test
    @DisplayName("tabela de transições: exatamente as 8 do enunciado")
    void tabelaDeTransicoes() {
        Set<String> permitidas = new HashSet<>();
        for (StatusInternacao de : StatusInternacao.values()) {
            for (StatusInternacao para : StatusInternacao.values()) {
                if (de.podeIrPara(para)) {
                    permitidas.add(de + "->" + para);
                }
            }
        }
        assertThat(permitidas).containsExactlyInAnyOrder(
                "ADMITIDA->EM_TRATAMENTO",
                "EM_TRATAMENTO->ISOLAMENTO",
                "EM_TRATAMENTO->ALTA_AUTORIZADA",
                "EM_TRATAMENTO->ALTA_A_PEDIDO_DO_TUTOR",
                "ISOLAMENTO->ALTA_AUTORIZADA",
                "ISOLAMENTO->ALTA_A_PEDIDO_DO_TUTOR",
                "ALTA_AUTORIZADA->ENCERRADA",
                "ALTA_A_PEDIDO_DO_TUTOR->ENCERRADA");
    }

    @Test
    @DisplayName("RN-08: só a internação encerrada deixa de ocupar a baia")
    void somenteEncerradaLiberaBaia() {
        for (StatusInternacao status : StatusInternacao.values()) {
            assertThat(status.ocupaBaia()).isEqualTo(status != ENCERRADA);
        }
    }

    @Test
    @DisplayName("admissão: status ADMITIDA e primeiro evento sem status anterior")
    void admissaoRegistraEvento() {
        Internacao internacao = admitida();

        assertThat(internacao.getStatus()).isEqualTo(ADMITIDA);
        assertThat(internacao.getDataAdmissao()).isEqualTo(T0);
        assertThat(internacao.getRegistradoPor()).isEqualTo(RECEPCAO);
        assertThat(internacao.getEventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getStatusAnterior()).isNull();
            assertThat(evento.getStatusNovo()).isEqualTo(ADMITIDA);
            assertThat(evento.getBaiaId()).isEqualTo(baiaColetiva);
            assertThat(evento.getUsuario()).isEqualTo(RECEPCAO);
        });
    }

    @Test
    @DisplayName("caminho principal: admitida → em tratamento → alta autorizada → encerrada, com um evento por passo")
    void caminhoPrincipal() {
        Internacao internacao = emTratamento();
        internacao.autorizarAlta(VET, T0.plusDays(2));
        internacao.encerrar(T0.plusDays(2).plusHours(1), RECEPCAO, T0.plusDays(2).plusHours(2));

        assertThat(internacao.getStatus()).isEqualTo(ENCERRADA);
        assertThat(internacao.getDataAlta()).isEqualTo(T0.plusDays(2));
        assertThat(internacao.getDataSaida()).isEqualTo(T0.plusDays(2).plusHours(1));
        assertThat(internacao.getEventos()).extracting(InternacaoEvento::getStatusNovo)
                .containsExactly(ADMITIDA, EM_TRATAMENTO, ALTA_AUTORIZADA, ENCERRADA);
        assertThat(internacao.getEventos()).extracting(InternacaoEvento::getUsuario)
                .containsExactly(RECEPCAO, VET, VET, RECEPCAO);
    }

    @Test
    @DisplayName("iniciar tratamento é idempotente: a segunda chamada não muda nada nem gera evento")
    void iniciarTratamentoIdempotente() {
        Internacao internacao = emTratamento();
        internacao.iniciarTratamento(VET, T0.plusHours(5));
        assertThat(internacao.getEventos()).hasSize(2);

        internacao.isolar(UUID.randomUUID(), VET, T0.plusHours(6));
        internacao.iniciarTratamento(VET, T0.plusHours(7));
        assertThat(internacao.getStatus()).isEqualTo(ISOLAMENTO);
        assertThat(internacao.getEventos()).hasSize(3);
    }

    @Test
    @DisplayName("isolar troca a baia e o evento registra a baia nova")
    void isolarTrocaBaia() {
        Internacao internacao = emTratamento();
        UUID baiaIsolamento = UUID.randomUUID();

        internacao.isolar(baiaIsolamento, VET, T0.plusHours(3));

        assertThat(internacao.getStatus()).isEqualTo(ISOLAMENTO);
        assertThat(internacao.getBaiaId()).isEqualTo(baiaIsolamento);
        assertThat(internacao.getEventos()).last().extracting(InternacaoEvento::getBaiaId).isEqualTo(baiaIsolamento);
    }

    @Test
    @DisplayName("do isolamento sai direto para a alta a pedido do tutor, com o termo gravado")
    void isolamentoParaAltaAPedido() {
        Internacao internacao = emTratamento();
        internacao.isolar(UUID.randomUUID(), VET, T0.plusHours(3));

        internacao.altaAPedidoDoTutor("Termo 123/2026", RECEPCAO, T0.plusHours(4));

        assertThat(internacao.getStatus()).isEqualTo(ALTA_A_PEDIDO_DO_TUTOR);
        assertThat(internacao.getTermoResponsabilidade()).isEqualTo("Termo 123/2026");
        assertThat(internacao.getDataAlta()).isEqualTo(T0.plusHours(4));
    }

    @Test
    @DisplayName("isolamento não volta para em tratamento")
    void isolamentoNaoVoltaParaTratamento() {
        assertThat(ISOLAMENTO.podeIrPara(EM_TRATAMENTO)).isFalse();
    }

    @Test
    @DisplayName("admitida não recebe alta sem passar pelo tratamento")
    void admitidaNaoRecebeAlta() {
        Internacao internacao = admitida();

        assertThatThrownBy(() -> internacao.autorizarAlta(VET, T0.plusHours(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ADMITIDA → ALTA_AUTORIZADA");
        assertThat(internacao.getStatus()).isEqualTo(ADMITIDA);
        assertThat(internacao.getEventos()).hasSize(1);
    }

    @Test
    @DisplayName("alta a pedido do tutor sem termo: recusada e nada muda")
    void altaAPedidoSemTermo() {
        Internacao internacao = emTratamento();

        assertThatThrownBy(() -> internacao.altaAPedidoDoTutor("  ", RECEPCAO, T0.plusHours(2)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("termo");
        assertThat(internacao.getStatus()).isEqualTo(EM_TRATAMENTO);
        assertThat(internacao.getDataAlta()).isNull();
        assertThat(internacao.getEventos()).hasSize(2);
    }

    @Test
    @DisplayName("RN-08: internação sem alta não pode ser encerrada (a baia continua ocupada)")
    void naoEncerraSemAlta() {
        Internacao internacao = emTratamento();

        assertThatThrownBy(() -> internacao.encerrar(T0.plusHours(2), RECEPCAO, T0.plusHours(3)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("EM_TRATAMENTO → ENCERRADA");
    }

    @Test
    @DisplayName("RN-08: encerrar exige o registro da saída física")
    void encerrarExigeSaida() {
        Internacao internacao = emTratamento();
        internacao.autorizarAlta(VET, T0.plusDays(1));

        assertThatThrownBy(() -> internacao.encerrar(null, RECEPCAO, T0.plusDays(1).plusHours(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("RN-08");
        assertThat(internacao.getStatus()).isEqualTo(ALTA_AUTORIZADA);
    }

    @Test
    @DisplayName("saída física antes da alta ou no futuro: recusada")
    void saidaForaDoIntervalo() {
        Internacao internacao = emTratamento();
        LocalDateTime alta = T0.plusDays(1);
        internacao.autorizarAlta(VET, alta);
        LocalDateTime agora = alta.plusHours(2);

        assertThatThrownBy(() -> internacao.encerrar(alta.minusMinutes(1), RECEPCAO, agora))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> internacao.encerrar(agora.plusMinutes(1), RECEPCAO, agora))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(internacao.getDataSaida()).isNull();
    }

    @Test
    @DisplayName("encerrada é estado final")
    void encerradaEhFinal() {
        Internacao internacao = emTratamento();
        internacao.autorizarAlta(VET, T0.plusDays(1));
        internacao.encerrar(T0.plusDays(1).plusHours(1), RECEPCAO, T0.plusDays(1).plusHours(1));

        assertThatThrownBy(() -> internacao.encerrar(T0.plusDays(1).plusHours(1), RECEPCAO, T0.plusDays(2)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> internacao.iniciarTratamento(VET, T0.plusDays(2)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
