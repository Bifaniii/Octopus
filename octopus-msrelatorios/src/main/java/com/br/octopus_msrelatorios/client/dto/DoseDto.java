package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;

// status: PENDENTE, APLICADA, CANCELADA ou PERDIDA. aplicadaPor é o e-mail do auxiliar. reaplicacaoDe: a dose
// perdida que esta reaplica (RN-07). seNecessario: aplicação de item sem horário fixo (mudança contratada de 01/11).
public record DoseDto(Long id, Long prescricaoId, Long internacaoId, Long medicacaoId, LocalDateTime horarioPrevisto,
                      String status, LocalDateTime aplicadaEm, String aplicadaPor, String observacao,
                      Long reaplicacaoDe, Boolean seNecessario) {

    /** Observação do relatório: o tipo da dose, quando não é um horário comum, seguido da observação do auxiliar. */
    public String observacaoDoRelatorio() {
        String tipo = Boolean.TRUE.equals(seNecessario) ? "Se necessário"
                : reaplicacaoDe != null ? "Reaplicação (RN-07)" : null;
        if (tipo == null) {
            return observacao;
        }
        return observacao == null || observacao.isBlank() ? tipo : tipo + ". " + observacao;
    }
}
