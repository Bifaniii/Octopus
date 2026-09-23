package com.br.octopus_msmedications.exception;

// Violação de uma regra de negócio (ex.: medicamento marcado como interação proibida de si mesmo).
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
