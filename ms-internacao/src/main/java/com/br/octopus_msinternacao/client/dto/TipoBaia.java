package com.br.octopus_msinternacao.client.dto;

// Cópia do enum Tipo do msbaias.
public enum TipoBaia {
    COLETIVA(1),
    ISOLAMENTO(1),
    NINHADA(6);

    private final int capacidadeMaxima;

    TipoBaia(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }
}
