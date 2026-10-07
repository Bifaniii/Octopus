package com.br.octopus_msbaias.domain.enums;

// Coletiva e isolamento até 1 animal; Ninhada até 6 animais da mesma família.
public enum Tipo {
    COLETIVA(1),
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
