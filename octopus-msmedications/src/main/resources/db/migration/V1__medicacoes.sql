CREATE TABLE tb_medicacoes (
    id                       BINARY(16)   NOT NULL,
    nome_comercial           VARCHAR(100) NOT NULL,
    principio_ativo          VARCHAR(200) NOT NULL,
    concentracao             VARCHAR(50)  NOT NULL,
    forma_farmaceutica       VARCHAR(50)  NOT NULL,
    unidade_medida_embalagem VARCHAR(20)  NOT NULL,
    tipo_esquema             VARCHAR(20)  NOT NULL,
    data_vencimento          DATETIME(6)  NOT NULL,
    fabricante               VARCHAR(255) NOT NULL,
    numero_registro_anvisa   VARCHAR(17)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_medicacoes_registro_anvisa (numero_registro_anvisa)
);

-- O par é gravado nos dois sentidos (A-B e B-A) pelo serviço.
CREATE TABLE tb_medicacao_interacoes (
    medicacao_id          BINARY(16) NOT NULL,
    medicacao_proibida_id BINARY(16) NOT NULL,
    PRIMARY KEY (medicacao_id, medicacao_proibida_id),
    KEY idx_interacoes_proibida (medicacao_proibida_id),
    CONSTRAINT fk_interacoes_medicacao FOREIGN KEY (medicacao_id) REFERENCES tb_medicacoes (id),
    CONSTRAINT fk_interacoes_proibida FOREIGN KEY (medicacao_proibida_id) REFERENCES tb_medicacoes (id)
);
