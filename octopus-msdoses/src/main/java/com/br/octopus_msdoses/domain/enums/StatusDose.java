package com.br.octopus_msdoses.domain.enums;

public enum StatusDose {
    PENDENTE,
    APLICADA,
    // A prescrição foi desativada ou o animal saiu antes do horário. A dose fica no histórico.
    CANCELADA,
    // RN-04: passou da janela de 30 minutos sem aplicação.
    PERDIDA
}
