package com.br.octopus_msrelatorios.exception;

/** Um dos módulos de onde vêm os dados não respondeu ou respondeu com erro. Responde 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
