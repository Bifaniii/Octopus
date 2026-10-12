package com.br.octopus_msrelatorios.exception;

/** Combinação de filtros sem sentido, como período com início depois do fim. Responde 400. */
public class FiltroInvalidoException extends RuntimeException {

    public FiltroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
