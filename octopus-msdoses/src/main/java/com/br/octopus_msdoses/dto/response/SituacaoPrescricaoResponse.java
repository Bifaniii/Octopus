package com.br.octopus_msdoses.dto.response;

/**
 * O que ainda resta de uma prescrição nas doses: quantas estão pendentes e se há item "se necessário" ativo. Sem
 * nenhuma das duas coisas, a prescrição não tem mais o que aplicar e o msprescricao a conclui.
 */
public record SituacaoPrescricaoResponse(Long prescricaoId, long pendentes, boolean seNecessarioAtivo) {
}
