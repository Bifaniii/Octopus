-- Id da baia passa de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad (só usuários continuam
-- UUID). As baias existentes são preservadas e renumeradas.
ALTER TABLE tb_baias ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE;

ALTER TABLE tb_baias
    DROP PRIMARY KEY,
    DROP COLUMN id;

ALTER TABLE tb_baias RENAME COLUMN id_novo TO id;

ALTER TABLE tb_baias
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    DROP KEY id_novo;
