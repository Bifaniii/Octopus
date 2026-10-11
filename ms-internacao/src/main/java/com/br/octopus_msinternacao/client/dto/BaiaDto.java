package com.br.octopus_msinternacao.client.dto;


// Só os campos que a internação usa; o resto do JSON do msbaias é ignorado.
public record BaiaDto(Long id, TipoBaia tipo, String nome, int capacidade, boolean ativo) {

    // RN-01: vale o menor entre a capacidade cadastrada e o máximo do tipo.
    public int capacidadeEfetiva() {
        return Math.min(capacidade, tipo.getCapacidadeMaxima());
    }
}
