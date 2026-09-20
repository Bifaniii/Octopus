package com.br.octopus_msbaias.exception;

// Violação de uma regra de negócio (ex.: capacidade acima do máximo do tipo, limite de baias atingido).
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
