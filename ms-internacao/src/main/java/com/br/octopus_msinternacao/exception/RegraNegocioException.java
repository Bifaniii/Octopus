package com.br.octopus_msinternacao.exception;

// Violação de uma regra de negócio (ex.: baia lotada, vacina antirrábica irregular fora do isolamento).
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
