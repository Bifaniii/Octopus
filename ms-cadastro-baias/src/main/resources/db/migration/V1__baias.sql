-- Baias da clínica: onde os animais ficam internados. `tipo` define a natureza (coletiva, isolamento,
-- ninhada) e `capacidade` o número máximo de animais. A ocupação/alocação em si é tratada na
-- internação (Sprint 2); aqui é só o cadastro.
CREATE TABLE tb_baias (
    id         BINARY(16)  NOT NULL,
    tipo       VARCHAR(20) NOT NULL,
    nome       VARCHAR(25) NOT NULL,
    descricao  VARCHAR(255),
    capacidade INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_baias_nome (nome)
);
