package com.br.octopus_msusuario.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NomeCompletoTest {

    private final NomeCompletoValidator validador = new NomeCompletoValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Maria Silva", "João da Silva", "Dra. Carla Souza", "Ana-Clara D'Ávila", "José A. Pereira",
            "  Paulo   Rangel  ", "Márcia Feitosa", "Zé Li"})
    void aceitaNomeESobrenomeSoComLetras(String nome) {
        assertThat(validador.isValid(nome, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Maria", "aaa12131241", "Maria Silva 2", "Pedro 3o Silva", "Ana#Souza", "João_Silva",
            "M. S.", "A B", "Maria S"})
    void recusaNomeComNumeroSimboloOuSemSobrenome(String nome) {
        var contexto = org.mockito.Mockito.mock(jakarta.validation.ConstraintValidatorContext.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        assertThat(validador.isValid(nome, contexto)).isFalse();
    }

    @Test
    void campoVazioFicaComOBlankDoCampo() {
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(validador.isValid("  ", null)).isTrue();
    }

    @Test
    void cnpjAlfanumericoDoExemploOficialEValidoEOsNumericosContinuam() {
        assertThat(Documentos.cnpjValido("12ABC34501DE35")).isTrue();
        assertThat(Documentos.cnpjValido("12ABC34501DE36")).isFalse(); // dígito errado
        assertThat(Documentos.cnpjValido("12abc34501de35")).isFalse(); // minúsculas não
        assertThat(Documentos.cnpjValido("12ABC34501DEAB")).isFalse(); // verificadores são números
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();  // numérico antigo
        assertThat(Documentos.cnpjValido("AAAAAAAAAAAAAA")).isFalse(); // tudo igual
        for (int i = 0; i < 50; i++) {
            assertThat(Documentos.cnpjValido(com.br.octopus_msusuario.GeradorDeDocumentos.cnpjAlfanumerico())).isTrue();
        }
    }
}
