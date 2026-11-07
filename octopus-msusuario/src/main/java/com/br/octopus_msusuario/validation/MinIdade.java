package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Idade mínima, em anos completos, calculada a partir da data de nascimento (LocalDate). Faz os anos no dia do
 * aniversário. Campo nulo é problema do @NotNull.
 */
@Documented
@Constraint(validatedBy = MinIdadeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinIdade {

    /** Veterinário: o mínimo de uma formação em medicina veterinária concluída. */
    int VETERINARIO = 23;

    /** Maioridade: auxiliar, recepcionista e tutor. */
    int ADULTO = 18;

    int value();

    String message() default "Idade mínima não atingida: são necessários pelo menos {value} anos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
