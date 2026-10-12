package com.br.octopus_msdoses.exception;

/** O octopus-msmedications não respondeu ou respondeu com erro inesperado. Responde 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
