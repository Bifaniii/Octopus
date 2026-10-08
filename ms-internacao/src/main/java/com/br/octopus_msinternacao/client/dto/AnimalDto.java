package com.br.octopus_msinternacao.client.dto;

import java.time.LocalDate;
import java.util.UUID;

// Só os campos que a internação usa; o resto do JSON do msusuario é ignorado.
public record AnimalDto(UUID id, String nome, String especie, LocalDate dataUltimaAntirrabica, UUID maeId) {
}
