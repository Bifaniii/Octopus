-- RN-07 e mudança contratada de 01/11. esquema: copiado do medicamento ao agendar (CONTINUO reaplica a dose
-- perdida, SINTOMATICO pula). reaplicacao_de: a dose perdida que esta reaplica. se_necessario: dose aplicada de um
-- item sem horário fixo. As doses anteriores a esta versão eram todas de horário fixo; CONTINUO é o caso mais
-- comum e o que não deixa de reaplicar.
ALTER TABLE tb_doses
    ADD COLUMN esquema        VARCHAR(20) NOT NULL DEFAULT 'CONTINUO' AFTER medicacao_id,
    ADD COLUMN se_necessario  BOOLEAN     NOT NULL DEFAULT FALSE AFTER esquema,
    ADD COLUMN reaplicacao_de BIGINT      NULL AFTER se_necessario,
    ADD KEY idx_doses_status_horario (status, horario_previsto);

CREATE TABLE tb_itens_se_necessario (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    item_prescricao_id     BIGINT      NOT NULL,
    prescricao_id          BIGINT      NOT NULL,
    internacao_id          BIGINT      NOT NULL,
    medicacao_id           BIGINT      NOT NULL,
    esquema                VARCHAR(20) NOT NULL,
    intervalo_minimo_horas INT         NOT NULL,
    ativo                  BOOLEAN     NOT NULL,
    versao                 BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_itens_se_necessario_item (item_prescricao_id),
    KEY idx_itens_se_necessario_prescricao (prescricao_id),
    KEY idx_itens_se_necessario_internacao (internacao_id)
);
