package com.br.octopus_msdoses.service;

import com.br.octopus_msdoses.client.EstoqueClient;
import com.br.octopus_msdoses.domain.Dose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reserva e devolução em lote, uma chamada por medicamento. Se uma das chamadas falha, desfaz as que já tinham
 * passado antes de repassar o erro, para o estoque não ficar pela metade.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class Estoque {

    private final EstoqueClient estoqueClient;

    static Map<Long, Integer> porMedicacao(Collection<Dose> doses) {
        Map<Long, Integer> contagem = new TreeMap<>();
        doses.forEach(d -> contagem.merge(d.getMedicacaoId(), 1, Integer::sum));
        return contagem;
    }

    void reservar(Map<Long, Integer> quantidades) {
        List<Map.Entry<Long, Integer>> feitas = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
                estoqueClient.reservar(e.getKey(), e.getValue());
                feitas.add(e);
            }
        } catch (RuntimeException falha) {
            feitas.forEach(e -> tentar(() -> estoqueClient.devolver(e.getKey(), e.getValue()), "devolver", e));
            throw falha;
        }
    }

    void devolver(Map<Long, Integer> quantidades) {
        List<Map.Entry<Long, Integer>> feitas = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
                estoqueClient.devolver(e.getKey(), e.getValue());
                feitas.add(e);
            }
        } catch (RuntimeException falha) {
            feitas.forEach(e -> tentar(() -> estoqueClient.reservar(e.getKey(), e.getValue()), "reservar", e));
            throw falha;
        }
    }

    private static void tentar(Runnable desfazer, String acao, Map.Entry<Long, Integer> e) {
        try {
            desfazer.run();
        } catch (RuntimeException ex) {
            log.error("Estoque do medicamento {} ficou inconsistente: não foi possível {} {} doses ao desfazer: {}",
                    e.getKey(), acao, e.getValue(), ex.getMessage());
        }
    }
}
