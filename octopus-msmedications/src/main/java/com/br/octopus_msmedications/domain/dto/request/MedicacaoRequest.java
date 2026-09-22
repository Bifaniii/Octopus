package com.br.octopus_msmedications.domain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime; 

public record MedicacaoRequest(
    @NotBlank(message = "O nome comercial é obrigatório.")
    @Size(max = 100, message = "O nome comercial deve ter no máximo 100 caracteres.")
    String nomeComercial,

    @NotBlank(message = "O princípio ativo é obrigatório.")
    @Size(max = 200, message = "O princípio ativo deve ter no máximo 200 caracteres.")
    String principioAtivo,
   
    @NotBlank(message = "A concentração é obrigatória.")
    @Size(max = 50, message = "A concentração deve ter no máximo 50 caracteres.")
    String concentracao,

    @NotBlank(message = "A forma farmacêutica é obrigatória.")
    @Size(max = 50, message = "A forma farmacêutica deve ter no máximo 50 caracteres.")
    String formaFarmaceutica,

    @NotBlank(message = "A unidade de medida da embalagem é obrigatória.")
    @Size(max = 50, message = "A unidade de medida da embalagem deve ter no máximo 20 caracteres.") 
    String unidadeMedidaEmbalagem,
    
    @NotNull(message = "A data de vencimento é obrigatória.")
    @Future(message = "A data de vencimento deve ser uma data futura.")
    LocalDateTime dataVencimento,

    @NotBlank(message = "O fabricante é obrigatório.")
    String fabricante,

    @NotBlank(message = "O número de registro da ANVISA é obrigatório.")
    @Pattern(regexp = "\\d{11}", message = "O registro ANVISA deve conter exatamente 11 dígitos numéricos.")
    String numeroRegistroAnvisa
){}
