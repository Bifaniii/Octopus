CREATE TABLE tb_medicacao(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome_comercial VARCHAR(100) NOT NULL,
    principio_ativo VARCHAR(200) NOT NULL,
    concentracao VARCHAR(50) NOT NULL,
    forma_farmaceutica VARCHAR (50) NOT NULL,
    unidade_medida_embalagem VARCHAR(20) NOT NULL,
    data_vencimento DATETIME NOT NULL,
    fabricante VARCHAR(255) NOT NULL,
    numero_registro_anvisa VARCHAR(17, NOT NULL)    
);

