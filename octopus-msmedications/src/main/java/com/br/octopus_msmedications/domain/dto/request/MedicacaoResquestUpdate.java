package com.br.octopus_msmedications.domain.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record MedicacaoResquestUpdate(

        @Size(max = 100, message = "O nome comercial deve ter no máximo 100 caracteres.")
        String nomeComercial,

        @Size(max = 200, message = "O princípio ativo deve ter no máximo 200 caracteres.")
        String principioAtivo,

        @Size(max = 50, message = "A concentração deve ter no máximo 50 caracteres.")
        String concentracao,

        @Size(max = 50, message = "A forma farmacêutica deve ter no máximo 50 caracteres.")
        String formaFarmaceutica,

        @Size(max = 50, message = "A unidade de medida da embalagem deve ter no máximo 20 caracteres.")
        String unidadeMedidaEmbalagem,

        @Future(message = "A data de vencimento deve ser uma data futura.")
        LocalDateTime dataVencimento,

        String fabricante,

        @Pattern(regexp = "\\d{11}", message = "O registro ANVISA deve conter exatamente 11 dígitos numéricos.")
        String numeroRegistroAnvisa
){}

