package com.br.octopus_msrelatorios.client.dto;

// tipo: COLETIVA, ISOLAMENTO ou NINHADA.
public record BaiaDto(Long id, String tipo, String nome, String descricao, int capacidade, boolean ativo) {
}
