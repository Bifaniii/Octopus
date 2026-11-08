package com.br.octopus_msusuario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

public class MaxIdadeValidator implements ConstraintValidator<MaxIdade, LocalDate> {

    // A data de hoje na clínica: o container roda em UTC e viraria o dia três horas antes.
    private static final ZoneId CLINICA = ZoneId.of("America/Sao_Paulo");

    private int maximo;

    @Override
    public void initialize(MaxIdade anotacao) {
        this.maximo = anotacao.value();
    }

    @Override
    public boolean isValid(LocalDate nascimento, ConstraintValidatorContext contexto) {
        if (nascimento == null) {
            return true;
        }
        LocalDate hoje = LocalDate.now(CLINICA);
        // Data no futuro é problema do @Past; aqui só se confere quem já nasceu.
        return nascimento.isAfter(hoje) || Period.between(nascimento, hoje).getYears() <= maximo;
    }
}
