package com.br.octopus_msmedications.domain.enums;

// Define o que acontece quando uma dose é perdida (Sprint 4, RN-07 e RN-08):
// CONTINUO desloca as doses seguintes; SINTOMATICO descarta a dose perdida.
public enum TipoEsquema {
    CONTINUO,
    SINTOMATICO
}
