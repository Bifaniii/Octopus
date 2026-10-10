-- Prescrição e seus itens. Todos os ids são BIGINT. internacao_id aponta para o ms-internacao e medicacao_id
-- para o octopus-msmedications: são tabelas de outros módulos, por isso só colunas indexadas, sem FOREIGN KEY.
CREATE TABLE tb_prescricoes (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    internacao_id     BIGINT       NOT NULL,
    veterinario_email VARCHAR(150) NOT NULL,
    observacao        VARCHAR(500) NULL,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_prescricoes_internacao (internacao_id)
);

CREATE TABLE tb_itens_prescricao (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    prescricao_id    BIGINT       NOT NULL,
    medicacao_id     BIGINT       NOT NULL,
    intervalo_horas  INT          NOT NULL,
    inicio           DATETIME(6)  NOT NULL,
    observacao       VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_itens_prescricao_prescricao (prescricao_id),
    KEY idx_itens_prescricao_medicacao (medicacao_id),
    CONSTRAINT fk_itens_prescricao_prescricao FOREIGN KEY (prescricao_id) REFERENCES tb_prescricoes (id)
);
