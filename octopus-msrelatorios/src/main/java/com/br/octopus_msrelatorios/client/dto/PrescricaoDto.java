package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;
import java.util.List;

// GET /api/prescricoes/{id} do octopus-msprescricao; veterinarioEmail é o e-mail de quem prescreveu.
public record PrescricaoDto(Long id, Long internacaoId, String veterinarioEmail, String observacao, boolean ativo,
                            LocalDateTime criadoEm, List<ItemDto> itens) {

    public record ItemDto(Long id, Long medicacaoId, String dosagem, boolean seNecessario, int intervaloHoras,
                          Integer quantidadeDoses, LocalDateTime inicio, String observacao) {
    }
}
