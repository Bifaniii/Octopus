package com.br.octopus_msusuario.domain.enums;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Espécies que a VidaPet atende (decidido em 07/11/2026). No banco fica o nome da constante (CAO, GATO, OUTRO); no
 * JSON vai o rótulo ("Cão", "Gato", "Outro"), que é o texto que o ms-internacao copia para a internação e que os
 * relatórios mostram. O nome da constante também é aceito na entrada.
 */
public enum Especie {
    @JsonAlias({"CAO", "Cao", "cão", "cao"})
    CAO("Cão"),
    @JsonAlias({"GATO", "gato"})
    GATO("Gato"),
    @JsonAlias({"OUTRO", "outro"})
    OUTRO("Outro");

    private final String rotulo;

    Especie(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String getRotulo() {
        return rotulo;
    }

    // O GlobalExceptionHandler lista os valores aceitos pelo toString quando chega uma espécie que não existe.
    @Override
    public String toString() {
        return rotulo;
    }
}
