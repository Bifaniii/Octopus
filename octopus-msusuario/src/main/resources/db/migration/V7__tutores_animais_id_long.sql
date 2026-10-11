-- Ids de tutor e animal passam de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad: só a conta
-- (tb_usuarios) e os perfis continuam UUID. Os registros existentes são preservados e renumerados, e as duas FKs
-- (tutor do animal e mãe) são remapeadas para os ids novos antes da troca das colunas.
ALTER TABLE tb_animais
    DROP FOREIGN KEY fk_animais_tutor,
    DROP FOREIGN KEY fk_animais_mae;

ALTER TABLE tb_tutores ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE;

ALTER TABLE tb_animais
    ADD COLUMN id_novo       BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
    ADD COLUMN tutor_id_novo BIGINT NULL,
    ADD COLUMN mae_id_novo   BIGINT NULL;

UPDATE tb_animais a JOIN tb_tutores t ON t.id = a.tutor_id SET a.tutor_id_novo = t.id_novo;
UPDATE tb_animais a JOIN tb_animais m ON m.id = a.mae_id SET a.mae_id_novo = m.id_novo;

ALTER TABLE tb_animais
    DROP PRIMARY KEY,
    DROP KEY idx_animais_tutor,
    DROP KEY idx_animais_mae,
    DROP COLUMN id,
    DROP COLUMN tutor_id,
    DROP COLUMN mae_id;

ALTER TABLE tb_animais
    RENAME COLUMN id_novo TO id,
    RENAME COLUMN tutor_id_novo TO tutor_id,
    RENAME COLUMN mae_id_novo TO mae_id;

ALTER TABLE tb_animais
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    MODIFY tutor_id BIGINT NOT NULL AFTER nome,
    ADD PRIMARY KEY (id),
    DROP KEY id_novo,
    ADD KEY idx_animais_tutor (tutor_id),
    ADD KEY idx_animais_mae (mae_id);

ALTER TABLE tb_tutores
    DROP PRIMARY KEY,
    DROP COLUMN id;

ALTER TABLE tb_tutores RENAME COLUMN id_novo TO id;

ALTER TABLE tb_tutores
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    DROP KEY id_novo;

ALTER TABLE tb_animais
    ADD CONSTRAINT fk_animais_tutor FOREIGN KEY (tutor_id) REFERENCES tb_tutores (id),
    ADD CONSTRAINT fk_animais_mae FOREIGN KEY (mae_id) REFERENCES tb_animais (id);
