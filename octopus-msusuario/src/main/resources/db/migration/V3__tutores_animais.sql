-- Tutor não faz login (sem usuario_id). Animal é o stub atual: espécie, vacina antirrábica etc. entram
-- em migrations futuras (ALTER TABLE tb_animais ADD COLUMN ...), nunca editando esta.

CREATE TABLE tb_tutores (
    id              BINARY(16)   NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    endereco        VARCHAR(255) NOT NULL,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE tb_animais (
    id       BINARY(16)   NOT NULL,
    nome     VARCHAR(100) NOT NULL,
    tutor_id BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_animais_tutor (tutor_id),
    CONSTRAINT fk_animais_tutor FOREIGN KEY (tutor_id) REFERENCES tb_tutores (id)
);
