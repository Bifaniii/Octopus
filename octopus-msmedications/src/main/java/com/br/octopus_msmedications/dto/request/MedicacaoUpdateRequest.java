package com.br.octopus_msmedications.dto.request;

import com.br.octopus_msmedications.domain.enums.TipoEsquema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

// Atualização parcial (PATCH): todo campo é opcional e só os preenchidos são aplicados.
public record MedicacaoUpdateRequest(

        @Size(max = 100, message = "O nome comercial deve ter no máximo 100 caracteres.")
        String nomeComercial,

        @Size(max = 200, message = "O princípio ativo deve ter no máximo 200 caracteres.")
        String principioAtivo,

        @Size(max = 50, message = "A concentração deve ter no máximo 50 caracteres.")
        String concentracao,

        @Size(max = 50, message = "A forma farmacêutica deve ter no máximo 50 caracteres.")
        String formaFarmaceutica,

        @Size(max = 20, message = "A unidade de medida da embalagem deve ter no máximo 20 caracteres.")
        String unidadeMedidaEmbalagem,

        TipoEsquema tipoEsquema,

        @Future(message = "A data de vencimento deve ser uma data futura.")
        LocalDateTime dataVencimento,

        @Size(max = 255, message = "O fabricante deve ter no máximo 255 caracteres.")
        String fabricante,

        @Pattern(regexp = "\\d{11}", message = "O registro ANVISA deve conter exatamente 11 dígitos numéricos.")
        String numeroRegistroAnvisa,

        Set<UUID> interacoesProibidas
) {
}
