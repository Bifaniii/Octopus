-- Id do medicamento passa de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad (só usuários
-- continuam UUID). Os medicamentos existentes são preservados e renumerados, e os pares de interação proibida
-- são remapeados para os ids novos antes da troca das colunas.
ALTER TABLE tb_medicacao_interacoes
    DROP FOREIGN KEY fk_interacoes_medicacao,
    DROP FOREIGN KEY fk_interacoes_proibida;

ALTER TABLE tb_medicacoes ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE;

ALTER TABLE tb_medicacao_interacoes
    ADD COLUMN medicacao_id_novo          BIGINT NULL,
    ADD COLUMN medicacao_proibida_id_novo BIGINT NULL;

UPDATE tb_medicacao_interacoes i JOIN tb_medicacoes m ON m.id = i.medicacao_id
SET i.medicacao_id_novo = m.id_novo;
UPDATE tb_medicacao_interacoes i JOIN tb_medicacoes m ON m.id = i.medicacao_proibida_id
SET i.medicacao_proibida_id_novo = m.id_novo;

ALTER TABLE tb_medicacao_interacoes
    DROP PRIMARY KEY,
    DROP KEY idx_interacoes_proibida,
    DROP COLUMN medicacao_id,
    DROP COLUMN medicacao_proibida_id;

ALTER TABLE tb_medicacao_interacoes
    RENAME COLUMN medicacao_id_novo TO medicacao_id,
    RENAME COLUMN medicacao_proibida_id_novo TO medicacao_proibida_id;

ALTER TABLE tb_medicacao_interacoes
    MODIFY medicacao_id BIGINT NOT NULL,
    MODIFY medicacao_proibida_id BIGINT NOT NULL,
    ADD PRIMARY KEY (medicacao_id, medicacao_proibida_id),
    ADD KEY idx_interacoes_proibida (medicacao_proibida_id);

ALTER TABLE tb_medicacoes
    DROP PRIMARY KEY,
    DROP COLUMN id;

ALTER TABLE tb_medicacoes RENAME COLUMN id_novo TO id;

ALTER TABLE tb_medicacoes
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    DROP KEY id_novo;

ALTER TABLE tb_medicacao_interacoes
    ADD CONSTRAINT fk_interacoes_medicacao FOREIGN KEY (medicacao_id) REFERENCES tb_medicacoes (id),
    ADD CONSTRAINT fk_interacoes_proibida FOREIGN KEY (medicacao_proibida_id) REFERENCES tb_medicacoes (id);
