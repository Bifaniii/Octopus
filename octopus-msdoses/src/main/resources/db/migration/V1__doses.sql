-- Uma linha por horário previsto de aplicação. prescricao_id, item_prescricao_id, internacao_id e medicacao_id
-- apontam para tabelas de outros módulos, por isso só colunas indexadas, sem FOREIGN KEY. O UNIQUE impede
-- agendar duas vezes o mesmo horário do mesmo item.
CREATE TABLE tb_doses (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    prescricao_id      BIGINT       NOT NULL,
    item_prescricao_id BIGINT       NOT NULL,
    internacao_id      BIGINT       NOT NULL,
    medicacao_id       BIGINT       NOT NULL,
    horario_previsto   DATETIME(6)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    aplicada_em        DATETIME(6)  NULL,
    aplicada_por       VARCHAR(150) NULL,
    observacao         VARCHAR(500) NULL,
    versao             BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_doses_item_horario (item_prescricao_id, horario_previsto),
    KEY idx_doses_prescricao (prescricao_id),
    KEY idx_doses_internacao_horario (internacao_id, horario_previsto)
);
