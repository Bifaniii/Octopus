-- Perfis de quem faz login. Não há herança: cada tabela tem seus próprios campos e uma FK 1:1 para
-- tb_usuarios (usuario_id UNIQUE garante que um usuário tenha no máximo um perfil de cada tipo).

CREATE TABLE tb_admins (
    id         BINARY(16)   NOT NULL,
    nome       VARCHAR(100) NOT NULL,
    usuario_id BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admins_usuario (usuario_id),
    CONSTRAINT fk_admins_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);

CREATE TABLE tb_veterinarios (
    id              BINARY(16)   NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    cpf_cnpj        VARCHAR(14)  NOT NULL,
    tipo_pessoa     VARCHAR(255) NOT NULL,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    crmv            VARCHAR(20)  NOT NULL,
    crmv_uf         VARCHAR(2)   NOT NULL,
    especializacao  VARCHAR(30)  NOT NULL,
    usuario_id      BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_veterinarios_cpf_cnpj (cpf_cnpj),
    UNIQUE KEY uk_veterinarios_crmv (crmv, crmv_uf),
    UNIQUE KEY uk_veterinarios_usuario (usuario_id),
    CONSTRAINT fk_veterinarios_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);

CREATE TABLE tb_auxiliares (
    id              BINARY(16)   NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    cpf             VARCHAR(11)  NOT NULL,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    usuario_id      BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auxiliares_cpf (cpf),
    UNIQUE KEY uk_auxiliares_usuario (usuario_id),
    CONSTRAINT fk_auxiliares_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);

CREATE TABLE tb_recepcionistas (
    id              BINARY(16)   NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    cpf             VARCHAR(11)  NOT NULL,
    data_nascimento DATE         NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    usuario_id      BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recepcionistas_cpf (cpf),
    UNIQUE KEY uk_recepcionistas_usuario (usuario_id),
    CONSTRAINT fk_recepcionistas_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);
