-- Cadastro de medicamentos (tela 3 do TAP): identificação do produto, tipo de esquema e interações
-- proibidas. Prescrição e doses são outros módulos (Sprint 3); aqui é só o cadastro.
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
    -- O registro ANVISA identifica o produto: não pode haver dois cadastros com o mesmo número.
    UNIQUE KEY uk_medicacoes_registro_anvisa (numero_registro_anvisa)
);

-- Pares de medicamentos que não podem ser prescritos juntos. A simetria (A-B e B-A) é gravada pelo
-- serviço, por isso as duas linhas do par existem e a PK composta impede duplicar o mesmo sentido.
CREATE TABLE tb_medicacao_interacoes (
    medicacao_id          BINARY(16) NOT NULL,
    medicacao_proibida_id BINARY(16) NOT NULL,
    PRIMARY KEY (medicacao_id, medicacao_proibida_id),
    KEY idx_interacoes_proibida (medicacao_proibida_id),
    CONSTRAINT fk_interacoes_medicacao FOREIGN KEY (medicacao_id) REFERENCES tb_medicacoes (id),
    CONSTRAINT fk_interacoes_proibida FOREIGN KEY (medicacao_proibida_id) REFERENCES tb_medicacoes (id)
);
