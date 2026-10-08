package com.br.octopus_msprescricao.dto.request;

import com.br.octopus_msprescricao.domain.Prescricao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// O veterinário não vem no corpo: é quem está no token (ver PrescricaoController).
public record PrescricaoRequest(
        @NotNull(message = "A internação é obrigatória")
        UUID internacaoId,

        @Size(max = 500)
        String observacao,

        @NotEmpty(message = "A prescrição precisa de ao menos um item")
        @Valid
        List<ItemPrescricaoRequest> itens
) {
    public Prescricao toEntity(String veterinarioEmail) {
        Prescricao prescricao = Prescricao.builder()
                .internacaoId(internacaoId)
                .veterinarioEmail(veterinarioEmail)
                .observacao(observacao)
                .build();
        itens.forEach(item -> prescricao.adicionarItem(item.toEntity()));
        return prescricao;
    }
}
