package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;

// GET /api/internacoes/{id}/eventos: cada mudança de status, com o e-mail de quem fez.
public record InternacaoEventoDto(String statusAnterior, String statusNovo, Long baiaId, String usuario,
                                  LocalDateTime dataHora) {
}
