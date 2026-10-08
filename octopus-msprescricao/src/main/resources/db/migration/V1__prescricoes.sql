-- Prescrição e seus itens. internacao_id e medicacao_id apontam para tabelas de outros módulos
-- (octopus-msinternacao e octopus-msmedications), por isso são só colunas indexadas, sem FOREIGN KEY.
CREATE TABLE tb_prescricoes (
    id                BINARY(16)   NOT NULL,
    internacao_id     BINARY(16)   NOT NULL,
    veterinario_email VARCHAR(150) NOT NULL,
    observacao        VARCHAR(500) NULL,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_prescricoes_internacao (internacao_id)
);

CREATE TABLE tb_itens_prescricao (
    id               BINARY(16)   NOT NULL,
    prescricao_id    BINARY(16)   NOT NULL,
    medicacao_id     BINARY(16)   NOT NULL,
    dosagem          VARCHAR(100) NOT NULL,
    intervalo_horas  INT          NOT NULL,
    quantidade_doses INT          NOT NULL,
    inicio           DATETIME(6)  NOT NULL,
    observacao       VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_itens_prescricao_prescricao (prescricao_id),
    KEY idx_itens_prescricao_medicacao (medicacao_id),
    CONSTRAINT fk_itens_prescricao_prescricao FOREIGN KEY (prescricao_id) REFERENCES tb_prescricoes (id)
);
