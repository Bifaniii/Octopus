package com.br.octopus_msusuario.exception;

/** O servidor de e-mail (SMTP) recusou ou não respondeu ao envio. Não é erro de quem chamou a API. */
public class EnvioDeEmailException extends RuntimeException {

    public EnvioDeEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
