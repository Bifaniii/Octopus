package com.br.octopus_msprescricao.client.dto;

// GET /api/doses/prescricoes/situacao do octopus-msdoses: o que ainda resta de cada prescrição.
public record SituacaoDosesDto(Long prescricaoId, long pendentes, boolean seNecessarioAtivo) {
}
