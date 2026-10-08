-- Mãe do animal (autorreferência, anulável). Usada pelo ms-internacao na RN-01: a baia de ninhada só aceita
-- filhotes da mesma mãe.
ALTER TABLE tb_animais
    ADD COLUMN mae_id BINARY(16) NULL,
    ADD KEY idx_animais_mae (mae_id),
    ADD CONSTRAINT fk_animais_mae FOREIGN KEY (mae_id) REFERENCES tb_animais (id);
