-- Ids da internação e do histórico passam de UUID (BINARY(16)) para BIGINT AUTO_INCREMENT, por decisão da squad
-- (internação, prescrição e dose usam Long; animal, baia e medicamento continuam UUID nos módulos deles).
-- As tabelas só tinham dados de teste, então são recriadas em vez de convertidas. O conteúdo é o da V1 com a V2.
DROP TABLE tb_internacao_eventos;
DROP TABLE tb_internacoes;

CREATE TABLE tb_internacoes (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    animal_id              BINARY(16)   NOT NULL,
    animal_nome            VARCHAR(100) NOT NULL,
    animal_especie         VARCHAR(250) NOT NULL,
    baia_id                BINARY(16)   NOT NULL,
    mae_id                 BINARY(16)   NULL,
    status                 VARCHAR(30)  NOT NULL,
    motivo                 VARCHAR(500) NOT NULL,
    termo_responsabilidade VARCHAR(500) NULL,
    data_admissao          DATETIME(6)  NOT NULL,
    data_alta              DATETIME(6)  NULL,
    data_saida             DATETIME(6)  NULL,
    registrado_por         VARCHAR(255) NOT NULL,
    versao                 BIGINT       NOT NULL,
    -- Preenchida só enquanto a internação está aberta; o UNIQUE garante uma internação aberta por animal.
    animal_internado       BINARY(16)   AS (CASE WHEN status <> 'ENCERRADA' THEN animal_id END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_internacoes_animal_internado (animal_internado),
    KEY idx_internacoes_baia_status (baia_id, status),
    KEY idx_internacoes_animal_status (animal_id, status)
);

CREATE TABLE tb_internacao_eventos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    internacao_id   BIGINT       NOT NULL,
    status_anterior VARCHAR(30)  NULL,
    status_novo     VARCHAR(30)  NOT NULL,
    baia_id         BINARY(16)   NOT NULL,
    usuario         VARCHAR(255) NOT NULL,
    data_hora       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_internacao_eventos_internacao (internacao_id),
    CONSTRAINT fk_internacao_eventos_internacao FOREIGN KEY (internacao_id) REFERENCES tb_internacoes (id)
);
