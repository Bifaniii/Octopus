package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Idade máxima, em anos completos, calculada a partir da data de nascimento (LocalDate): com 70 como limite, quem
 * faz 71 anos no dia do aniversário deixa de valer. Campo nulo é problema do @NotNull.
 */
@Documented
@Constraint(validatedBy = MaxIdadeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxIdade {

    /** Quem trabalha na clínica (veterinário, auxiliar e recepcionista). */
    int FUNCIONARIO = 70;

    /** Tutor: não tem teto de trabalho, então o limite só barra data absurda (como nascer há 200 anos). */
    int TUTOR = 120;

    int value();

    String message() default "Idade máxima excedida: o limite é de {value} anos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
