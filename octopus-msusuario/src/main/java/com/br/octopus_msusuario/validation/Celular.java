package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Celular brasileiro, só dígitos: DDD que existe + 9 + 8 dígitos (11 no total). Telefone fixo não é aceito por
 * enquanto. Campo nulo é problema do @NotBlank.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = Celular.REGEX, message = "Celular inválido: informe DDD e 9 dígitos, como 11987654321")
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Celular {

    /** DDDs de 11 a 99 que existem no país, depois o 9 do celular e mais 8 dígitos. */
    String REGEX = "(1[1-9]|2[12478]|3[1-578]|4[1-9]|5[1345]|6[1-9]|7[134579]|8[1-9]|9[1-9])9\\d{8}";

    String message() default "Celular inválido: informe DDD e 9 dígitos, como 11987654321";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
