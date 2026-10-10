package com.br.octopus_msprescricao.client.dto;

import java.util.List;


// Só o que a prescrição usa da MedicacaoResponse do octopus-msmedications; o resto do JSON é ignorado.
// As interações proibidas são gravadas lá nos dois sentidos do par (se A proíbe B, B proíbe A).
public record MedicacaoDto(Long id, String nomeComercial, boolean ativo, List<Interacao> interacoesProibidas) {

    public record Interacao(Long id, String nomeComercial) {
    }

    /** Nome do medicamento com quem este tem interação proibida, ou null se não tiver. */
    public String nomeDaInteracaoCom(Long outro) {
        if (interacoesProibidas == null) {
            return null;
        }
        return interacoesProibidas.stream()
                .filter(i -> i.id().equals(outro))
                .map(Interacao::nomeComercial)
                .findFirst()
                .orElse(null);
    }
}
