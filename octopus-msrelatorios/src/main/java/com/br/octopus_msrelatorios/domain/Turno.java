package com.br.octopus_msrelatorios.domain;

public enum Turno {

    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite");

    private final String rotulo;

    Turno(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
