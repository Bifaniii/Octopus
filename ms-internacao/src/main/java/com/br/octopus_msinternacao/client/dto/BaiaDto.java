package com.br.octopus_msinternacao.client.dto;

import java.util.UUID;

// Só os campos que a internação usa; o resto do JSON do msbaias é ignorado.
public record BaiaDto(UUID id, TipoBaia tipo, String nome, int capacidade, boolean ativo) {

    // RN-01: vale o menor entre a capacidade cadastrada e o máximo do tipo.
    public int capacidadeEfetiva() {
        return Math.min(capacidade, tipo.getCapacidadeMaxima());
    }
}
