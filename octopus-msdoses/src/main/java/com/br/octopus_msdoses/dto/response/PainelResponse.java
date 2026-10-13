package com.br.octopus_msdoses.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// Tela 6. atrasadas: pendentes que passaram do horário e ainda estão na janela (RN-03). proximas: pendentes que
// vencem na próxima hora. perdidas: das últimas 24 horas (RN-04). seNecessario: fora do painel de atrasos, com o
// horário a partir do qual a próxima aplicação é aceita.
public record PainelResponse(
        LocalDateTime agora,
        List<DoseResponse> atrasadas,
        List<DoseResponse> proximas,
        List<DoseResponse> perdidas,
        List<ItemSeNecessarioResponse> seNecessario
) {
}
