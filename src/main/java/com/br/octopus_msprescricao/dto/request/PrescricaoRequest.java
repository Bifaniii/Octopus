package com.br.octopus_msprescricao.dto.request;

import com.br.octopus_msprescricao.domain.Prescricao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

// O veterinário não vem no corpo: é quem está no token (ver PrescricaoController).
public record PrescricaoRequest(
        @NotNull(message = "A internação é obrigatória")
        Long internacaoId,

        @Size(max = 500)
        String observacao,

        @NotEmpty(message = "A prescrição precisa de ao menos um item")
        @Valid
        List<ItemPrescricaoRequest> itens
) {
    public Prescricao toEntity(String veterinarioEmail, LocalDateTime criadoEm) {
        Prescricao prescricao = Prescricao.builder()
                .internacaoId(internacaoId)
                .veterinarioEmail(veterinarioEmail)
                .observacao(observacao)
                .criadoEm(criadoEm)
                .build();
        itens.forEach(item -> prescricao.adicionarItem(item.toEntity()));
        return prescricao;
    }
}
