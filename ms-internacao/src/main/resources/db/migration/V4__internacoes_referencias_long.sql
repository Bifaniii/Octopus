-- Animal, baia e mãe passam a BIGINT, acompanhando a troca de ids no msusuario e no msbaias (decisão da squad: só
-- usuários continuam UUID). Os UUIDs gravados aqui não têm correspondência nos módulos de origem, que renumeraram
-- os registros, então as internações de teste gravadas até agora são descartadas.
DELETE FROM tb_internacao_eventos;
DELETE FROM tb_internacoes;

ALTER TABLE tb_internacoes
    DROP KEY uk_internacoes_animal_internado,
    DROP COLUMN animal_internado;

ALTER TABLE tb_internacoes
    MODIFY animal_id BIGINT NOT NULL,
    MODIFY baia_id   BIGINT NOT NULL,
    MODIFY mae_id    BIGINT NULL,
    -- Preenchida só enquanto a internação está aberta; o UNIQUE garante uma internação aberta por animal.
    ADD COLUMN animal_internado BIGINT AS (CASE WHEN status <> 'ENCERRADA' THEN animal_id END) STORED,
    ADD UNIQUE KEY uk_internacoes_animal_internado (animal_internado);

ALTER TABLE tb_internacao_eventos MODIFY baia_id BIGINT NOT NULL;
