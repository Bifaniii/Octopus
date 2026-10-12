package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;

// status: PENDENTE, APLICADA ou CANCELADA. aplicadaPor é o e-mail do auxiliar.
public record DoseDto(Long id, Long prescricaoId, Long internacaoId, Long medicacaoId, LocalDateTime horarioPrevisto,
                      String status, LocalDateTime aplicadaEm, String aplicadaPor, String observacao) {
}
