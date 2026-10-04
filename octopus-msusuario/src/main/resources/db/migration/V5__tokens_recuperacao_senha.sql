-- Códigos de redefinição de senha. Nada é apagado: o token usado vira USADO e o substituído por um
-- pedido novo vira ARQUIVADO, então o histórico de pedidos fica registrado.
CREATE TABLE tb_tokens_recuperacao_senha (
    id         BINARY(16)   NOT NULL,
    usuario_id BINARY(16)   NOT NULL,
    token      VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    expira_em  DATETIME(6)  NOT NULL,
    criado_em  DATETIME(6)  NOT NULL,
    usado_em   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tokens_recuperacao_token (token),
    KEY idx_tokens_recuperacao_usuario (usuario_id, status),
    CONSTRAINT fk_tokens_recuperacao_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);
