-- medicacao_id passa a BIGINT, acompanhando a troca de ids no octopus-msmedications (decisão da squad: só usuários
-- continuam UUID). Os UUIDs gravados não têm correspondência lá, que renumerou os medicamentos, e as internações a
-- que estas prescrições apontavam também foram descartadas no ms-internacao (V4), então as prescrições de teste
-- gravadas até agora são descartadas.
DELETE FROM tb_itens_prescricao;
DELETE FROM tb_prescricoes;

ALTER TABLE tb_itens_prescricao MODIFY medicacao_id BIGINT NOT NULL;
