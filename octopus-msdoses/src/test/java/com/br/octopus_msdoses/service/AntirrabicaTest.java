package com.br.octopus_msdoses.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AntirrabicaTest {

    @ParameterizedTest
    @ValueSource(strings = {"Antirrábica", "ANTIRRABICA", "Vacina antirrábica", "anti-rábica", "Anti Rábica Canina"})
    void reconheceAVacinaPeloNome(String nome) {
        assertThat(Antirrabica.ehAntirrabica(nome)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dipirona", "Antibiótico A", "Vacina V10", ""})
    void naoConfundeComOutrosMedicamentos(String nome) {
        assertThat(Antirrabica.ehAntirrabica(nome)).isFalse();
    }
}
