-- Ids de tutor e animal passam de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad: só a conta
-- (tb_usuarios) e os perfis continuam UUID. Os registros existentes são preservados e renumerados, e as duas FKs
-- (tutor do animal e mãe) são remapeadas para os ids novos antes da troca das colunas.
--
-- Duas exigências do MySQL do Aiven (produção) moldam este arquivo:
-- 1. sql_require_primary_key: nenhuma tabela pode ficar sem PK, nem entre dois comandos. Por isso a PK antiga sai
--    e a nova entra no MESMO ALTER.
-- 2. Em 12/10 a primeira versão desta migration falhou lá depois de remover as FKs e criar as colunas *_novo (DDL
--    no MySQL não volta atrás). Esses primeiros passos só rodam se ainda forem necessários, para a migration
--    seguir tanto de um banco intacto quanto desse estado parcial.

SET @s = IF((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
             AND TABLE_NAME = 'tb_animais' AND CONSTRAINT_NAME = 'fk_animais_tutor') > 0,
            'ALTER TABLE tb_animais DROP FOREIGN KEY fk_animais_tutor', 'DO 0');
PREPARE passo FROM @s;
EXECUTE passo;
DEALLOCATE PREPARE passo;

SET @s = IF((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
             AND TABLE_NAME = 'tb_animais' AND CONSTRAINT_NAME = 'fk_animais_mae') > 0,
            'ALTER TABLE tb_animais DROP FOREIGN KEY fk_animais_mae', 'DO 0');
PREPARE passo FROM @s;
EXECUTE passo;
DEALLOCATE PREPARE passo;

SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'tb_tutores' AND COLUMN_NAME = 'id_novo') = 0,
            'ALTER TABLE tb_tutores ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE', 'DO 0');
PREPARE passo FROM @s;
EXECUTE passo;
DEALLOCATE PREPARE passo;

SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'tb_animais' AND COLUMN_NAME = 'id_novo') = 0,
            'ALTER TABLE tb_animais ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE, ADD COLUMN tutor_id_novo BIGINT NULL, ADD COLUMN mae_id_novo BIGINT NULL',
            'DO 0');
PREPARE passo FROM @s;
EXECUTE passo;
DEALLOCATE PREPARE passo;

UPDATE tb_animais a JOIN tb_tutores t ON t.id = a.tutor_id SET a.tutor_id_novo = t.id_novo;
UPDATE tb_animais a JOIN tb_animais m ON m.id = a.mae_id SET a.mae_id_novo = m.id_novo;

ALTER TABLE tb_animais
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id_novo),
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
    DROP KEY id_novo,
    ADD KEY idx_animais_tutor (tutor_id),
    ADD KEY idx_animais_mae (mae_id);

ALTER TABLE tb_tutores
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id_novo),
    DROP COLUMN id;

ALTER TABLE tb_tutores RENAME COLUMN id_novo TO id;

ALTER TABLE tb_tutores
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    DROP KEY id_novo;

ALTER TABLE tb_animais
    ADD CONSTRAINT fk_animais_tutor FOREIGN KEY (tutor_id) REFERENCES tb_tutores (id),
    ADD CONSTRAINT fk_animais_mae FOREIGN KEY (mae_id) REFERENCES tb_animais (id);
