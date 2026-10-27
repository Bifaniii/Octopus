package com.br.octopus_msusuario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.regex.Pattern;

public class NomeCompletoValidator implements ConstraintValidator<NomeCompleto, String> {

    /** Letras de qualquer idioma, espaço, apóstrofo, hífen e ponto (como em "D'Ávila", "Maria-José" e "Dra."). */
    private static final Pattern CARACTERES = Pattern.compile("[\\p{L}\\p{M}\\s'’.-]+");

    @Override
    public boolean isValid(String nome, ConstraintValidatorContext contexto) {
        if (nome == null || nome.isBlank()) {
            return true;
        }
        String texto = nome.strip();
        if (!CARACTERES.matcher(texto).matches()) {
            return recusar(contexto, "Nome inválido: não pode ter números nem símbolos");
        }
        long palavras = Arrays.stream(texto.split("\\s+"))
                .filter(p -> p.codePoints().filter(Character::isLetter).count() >= 2)
                .count();
        if (palavras < 2) {
            return recusar(contexto, "Nome inválido: informe nome e sobrenome");
        }
        return true;
    }

    private static boolean recusar(ConstraintValidatorContext contexto, String mensagem) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(mensagem).addConstraintViolation();
        return false;
    }
}
