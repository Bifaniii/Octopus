package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nome de pessoa: só letras (com acento), espaço, apóstrofo, hífen e ponto, sem números, e com nome e sobrenome
 * (pelo menos duas palavras de 2 letras ou mais). Campo nulo ou vazio é problema do @NotBlank.
 */
@Documented
@Constraint(validatedBy = NomeCompletoValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NomeCompleto {

    String message() default "Nome inválido: informe nome e sobrenome, só com letras";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
