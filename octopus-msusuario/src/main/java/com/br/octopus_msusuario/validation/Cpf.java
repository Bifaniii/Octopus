package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CPF com 11 dígitos, sem máscara, e dígitos verificadores certos. Campo nulo é problema do @NotBlank. */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

    String message() default "CPF inválido: informe os 11 dígitos, sem pontos nem traço";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
