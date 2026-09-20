package com.br.octopus_msbaias.domain.enums;

// coletiva e ninhada até 6; isolamento é individual (1 animal).
public enum Tipo {
    COLETIVA(6),
    ISOLAMENTO(1),
    NINHADA(6);

    private final int capacidadeMaxima;

    Tipo(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }
}
