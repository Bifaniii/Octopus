package com.br.octopus_medications.domain.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Future;

import java.time.LocalDateTime; 

public record MedicacaoRequest(
    @NotBlank(message = "O nome comercial é obrigatório.")
    @Size(max = 100, message = "O nome comercial deve ter no máximo 100 caracteres")
    String nomeComercial,

    @NotBlank(message = "O princípio ativo é obrigatório.")
    @Size(max = 200, message = "O princípio ativo deve ter no máximo 200 caracteres")
    String principioAtivo,

    
    String concentracao,
    String unidadeMedidaEmbalagem,
    LocalDateTime dataVencimento,
    String fabricante,
    String numeroRegistroAnvisa
){}
