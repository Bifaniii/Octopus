-- Horários gerados a partir de cada item prescrito. O índice (item_id, horario_previsto) serve tanto à
-- leitura ordenada de um item quanto ao painel de doses da Sprint 3.
CREATE TABLE tb_doses (
    id               BINARY(16)  NOT NULL,
    item_id          BINARY(16)  NOT NULL,
    horario_previsto DATETIME(6) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_doses_item_horario (item_id, horario_previsto),
    CONSTRAINT fk_doses_item FOREIGN KEY (item_id) REFERENCES tb_itens_prescricao (id)
);
