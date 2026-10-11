package com.br.octopus_msinternacao.client.dto;

import java.time.LocalDate;

// Só os campos que a internação usa; o resto do JSON do msusuario é ignorado.
public record AnimalDto(Long id, String nome, String especie, LocalDate dataUltimaAntirrabica, Long maeId) {
}
