package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.exception.FiltroInvalidoException;
import com.br.octopus_msrelatorios.relatorio.Valores;

import java.time.LocalDate;

final class Periodos {

    private Periodos() {
    }

    static void validar(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new FiltroInvalidoException("O início do período não pode ser depois do fim.");
        }
    }

    static String descricao(LocalDate de, LocalDate ate) {
        if (de == null && ate == null) {
            return "Período: todos";
        }
        if (de == null) {
            return "Período: até " + Valores.texto(ate);
        }
        if (ate == null) {
            return "Período: a partir de " + Valores.texto(de);
        }
        return de.equals(ate) ? "Período: " + Valores.texto(de)
                : "Período: " + Valores.texto(de) + " a " + Valores.texto(ate);
    }

    static boolean dentro(LocalDate data, LocalDate de, LocalDate ate) {
        return data != null && (de == null || !data.isBefore(de)) && (ate == null || !data.isAfter(ate));
    }
}
