-- Conta de login. Cada perfil (admin, veterinário, auxiliar, recepcionista) aponta para uma linha daqui
-- via usuario_id (composição, não herança): ver V2__perfis.sql.
CREATE TABLE tb_usuarios (
    id        BINARY(16)   NOT NULL,
    email     VARCHAR(150) NOT NULL,
    senha     VARCHAR(255) NOT NULL,
    role      VARCHAR(30)  NOT NULL,
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
);
