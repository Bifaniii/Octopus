-- Id da baia passa de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad (só usuários continuam
-- UUID). As baias existentes são preservadas e renumeradas.
-- O MySQL do Aiven (produção) exige chave primária em toda tabela (sql_require_primary_key), nem que seja entre dois
-- comandos: a PK antiga sai e a nova entra no mesmo ALTER.
ALTER TABLE tb_baias ADD COLUMN id_novo BIGINT NOT NULL AUTO_INCREMENT UNIQUE;

ALTER TABLE tb_baias
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id_novo),
    DROP COLUMN id;

ALTER TABLE tb_baias RENAME COLUMN id_novo TO id;

ALTER TABLE tb_baias
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    DROP KEY id_novo;
