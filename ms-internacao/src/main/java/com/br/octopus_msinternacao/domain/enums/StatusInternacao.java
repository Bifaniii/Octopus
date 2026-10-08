package com.br.octopus_msinternacao.domain.enums;

// Ciclo de vida da internação, conforme o enunciado do projeto.
public enum StatusInternacao {
    ADMITIDA,
    EM_TRATAMENTO,
    ISOLAMENTO,
    ALTA_AUTORIZADA,
    ALTA_A_PEDIDO_DO_TUTOR,
    ENCERRADA;

    public boolean podeIrPara(StatusInternacao destino) {
        return switch (this) {
            case ADMITIDA -> destino == EM_TRATAMENTO;
            case EM_TRATAMENTO -> destino == ISOLAMENTO || destino == ALTA_AUTORIZADA || destino == ALTA_A_PEDIDO_DO_TUTOR;
            case ISOLAMENTO -> destino == ALTA_AUTORIZADA || destino == ALTA_A_PEDIDO_DO_TUTOR;
            case ALTA_AUTORIZADA, ALTA_A_PEDIDO_DO_TUTOR -> destino == ENCERRADA;
            case ENCERRADA -> false;
        };
    }

    // RN-08: a baia só é liberada com a saída física, ou seja, quando a internação é encerrada.
    public boolean ocupaBaia() {
        return this != ENCERRADA;
    }
}
