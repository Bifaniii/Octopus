-- Mudança contratada de 01/11/2026: item "se necessário", sem horário fixo. Ele guarda o intervalo mínimo em
-- intervalo_horas e não tem quantidade de doses nem início.
ALTER TABLE tb_itens_prescricao
    ADD COLUMN se_necessario BOOLEAN NOT NULL DEFAULT FALSE AFTER dosagem,
    MODIFY quantidade_doses INT NULL,
    MODIFY inicio DATETIME(6) NULL;
