-- Cópia do nome e da espécie do animal no momento da admissão, para o painel mostrar quem está internado sem
-- consultar o msusuario (o GET /api/animais é restrito a ADMIN e RECEPCIONISTA, e o painel é visto por todos).
ALTER TABLE tb_internacoes
    ADD COLUMN animal_nome    VARCHAR(100) NOT NULL AFTER animal_id,
    ADD COLUMN animal_especie VARCHAR(250) NOT NULL AFTER animal_nome;
