package com.br.octopus_msusuario.exception;


public class AnimalNaoEncontradoException extends RuntimeException{
    public AnimalNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
