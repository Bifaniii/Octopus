package com.br.octopus_msrelatorios.exception;

/** O msusuario não conseguiu entregar o e-mail ao servidor SMTP (conta de envio, senha de app ou rede). */
public class EmailNaoEnviadoException extends RuntimeException {

    public EmailNaoEnviadoException(String mensagem) {
        super(mensagem);
    }
}
