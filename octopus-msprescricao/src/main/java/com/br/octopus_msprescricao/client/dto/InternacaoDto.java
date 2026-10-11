package com.br.octopus_msprescricao.client.dto;

// Só o que a prescrição usa da InternacaoResponse do ms-internacao; o resto do JSON é ignorado. O status fica
// como texto para este módulo não depender do enum de lá.
public record InternacaoDto(Long id, String status, String animalNome) {
}
