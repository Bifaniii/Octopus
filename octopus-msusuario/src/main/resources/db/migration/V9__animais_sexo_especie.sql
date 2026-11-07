-- Sexo do animal e espécie em lista fixa (07/11/2026). Só uma fêmea da mesma espécie pode ser indicada como mãe,
-- que é o que o ms-internacao confere na RN-01 (baia de ninhada).
--
-- Sexo: anulável, porque os animais já cadastrados não têm essa informação; o cadastro e a edição passam a exigir.
ALTER TABLE tb_animais
    ADD COLUMN sexo VARCHAR(10) NULL;

-- Espécie: o texto livre vira CAO, GATO ou OUTRO. A comparação usa a collation da coluna, que no MySQL 8 ignora
-- acento e caixa ("cão", "Cao" e "CÃO" caem juntos). O animal sem espécie da carga do enunciado fica NULL, e o
-- ms-internacao continua recusando a internação dele.
ALTER TABLE tb_animais
    MODIFY COLUMN especie VARCHAR(250) NULL;

UPDATE tb_animais
SET especie = CASE
        WHEN especie IS NULL OR TRIM(especie) = '' THEN NULL
        WHEN TRIM(especie) IN ('cão', 'cao', 'cães', 'caes', 'cachorro', 'cachorra', 'cadela', 'canino', 'canina', 'dog')
            THEN 'CAO'
        WHEN TRIM(especie) IN ('gato', 'gata', 'gatos', 'felino', 'felina', 'cat') THEN 'GATO'
        ELSE 'OUTRO'
    END;
