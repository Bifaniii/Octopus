package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;
import java.util.List;

// GET /api/prescricoes/{id} do octopus-msprescricao; veterinarioEmail é o e-mail de quem prescreveu.
public record PrescricaoDto(Long id, Long internacaoId, String veterinarioEmail, String observacao, boolean ativo, String situacao,
                            LocalDateTime criadoEm, List<ItemDto> itens) {

    /** O veterinário a desativou à mão (a que acabou sozinha é CONCLUIDA). Sem `situacao`, vale o `ativo`. */
    public boolean desativada() {
        return situacao != null ? "DESATIVADA".equals(situacao) : !ativo;
    }

    public boolean concluida() {
        return "CONCLUIDA".equals(situacao);
    }

    public record ItemDto(Long id, Long medicacaoId, String dosagem, boolean seNecessario, int intervaloHoras,
                          Integer quantidadeDoses, LocalDateTime inicio, String observacao) {
    }
}
