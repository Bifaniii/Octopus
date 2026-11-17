package com.br.octopus_msprescricao.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenRepassadoTest {

    @Test
    void foraDeUmaRequisicaoNaoHaToken() {
        assertThat(TokenRepassado.daRequisicaoAtual()).isNull();
        assertThat(TokenRepassado.daTarefaAtual()).isNull();
    }

    @Test
    void aTarefaEnxergaOTokenGuardadoESomeAoTerminar() {
        String visto = TokenRepassado.executarCom("Bearer abc", TokenRepassado::daTarefaAtual);

        assertThat(visto).isEqualTo("Bearer abc");
        assertThat(TokenRepassado.daTarefaAtual()).isNull();
    }

    @Test
    void oTokenSomeMesmoQuandoATarefaFalha() {
        assertThatThrownBy(() -> TokenRepassado.executarCom("Bearer abc", () -> {
            throw new IllegalStateException("falhou");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(TokenRepassado.daTarefaAtual()).isNull();
    }
}
