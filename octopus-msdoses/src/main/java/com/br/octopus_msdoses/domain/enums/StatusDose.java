package com.br.octopus_msdoses.domain.enums;

public enum StatusDose {
    PENDENTE,
    APLICADA,
    // A prescrição foi desativada antes do horário. A dose fica no histórico.
    CANCELADA
}
