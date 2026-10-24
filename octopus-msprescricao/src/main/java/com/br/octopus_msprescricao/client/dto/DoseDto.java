package com.br.octopus_msprescricao.client.dto;

// Só o que a prescrição usa da DoseResponse do octopus-msdoses: o status (PENDENTE, APLICADA, CANCELADA, PERDIDA).
public record DoseDto(Long id, String status) {
}
