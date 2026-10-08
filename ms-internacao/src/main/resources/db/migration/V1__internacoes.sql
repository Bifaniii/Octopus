-- Internações: um animal (msusuario) alocado numa baia (msbaias). animal_id, baia_id e mae_id não têm FK
-- porque as tabelas são de outros módulos; a existência é conferida via REST na admissão. A baia fica ocupada
-- enquanto status <> 'ENCERRADA' (RN-08).
-- mae_id é uma cópia da mãe do animal no momento da admissão, para a RN-01 da ninhada ser conferida dentro da
-- trava da baia sem chamada REST.
-- animal_internado é coluna gerada (não mapeada na entidade): vale animal_id enquanto a internação está
-- aberta e NULL depois de encerrada. O UNIQUE garante no banco no máximo uma internação aberta por animal,
-- mesmo com duas admissões simultâneas (NULL não conflita no UNIQUE).
CREATE TABLE tb_internacoes (
    id                     BINARY(16)   NOT NULL,
    animal_id              BINARY(16)   NOT NULL,
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
    animal_internado       BINARY(16)   AS (CASE WHEN status <> 'ENCERRADA' THEN animal_id END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_internacoes_animal_internado (animal_internado),
    KEY idx_internacoes_baia_status (baia_id, status),
    KEY idx_internacoes_animal_status (animal_id, status)
);

-- Histórico da internação: uma linha por mudança de status ou de baia. Nada é apagado nem editado.
CREATE TABLE tb_internacao_eventos (
    id              BINARY(16)   NOT NULL,
    internacao_id   BINARY(16)   NOT NULL,
    status_anterior VARCHAR(30)  NULL,
    status_novo     VARCHAR(30)  NOT NULL,
    baia_id         BINARY(16)   NOT NULL,
    usuario         VARCHAR(255) NOT NULL,
    data_hora       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_internacao_eventos_internacao (internacao_id),
    CONSTRAINT fk_internacao_eventos_internacao FOREIGN KEY (internacao_id) REFERENCES tb_internacoes (id)
);
