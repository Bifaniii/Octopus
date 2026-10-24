package com.br.octopus_msusuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pessoa física precisa de um CPF válido e pessoa jurídica, de um CNPJ válido. O erro aparece no campo `cpfCnpj`.
 */
@Documented
@Constraint(validatedBy = DocumentoCompativelValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentoCompativel {

    String message() default "Documento inválido para o tipo de pessoa";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
