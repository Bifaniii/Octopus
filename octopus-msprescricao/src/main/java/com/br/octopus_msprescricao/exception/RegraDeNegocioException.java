package com.br.octopus_msprescricao.exception;

/** Pedido bem formado que quebra uma regra do TAP (ex.: RN-03, interação proibida). Responde 422. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
