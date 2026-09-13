package com.br.octopus_msusuario.exception;

public class LoginNaoPermitidoException extends RuntimeException {

    public LoginNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
