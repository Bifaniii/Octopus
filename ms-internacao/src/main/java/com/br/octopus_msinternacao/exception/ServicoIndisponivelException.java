package com.br.octopus_msinternacao.exception;

// Outro módulo fora do ar, lento ou recusando a consulta: 503, porque não é erro de quem chamou.
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
