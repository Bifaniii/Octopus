package com.br.octopus_msinternacao.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Baia de isolamento para onde o animal vai (pode ser a mesma, se ele já foi admitido no isolamento).
public record IsolamentoRequest(@NotNull UUID baiaId) {
}
