-- Prescrição que acabou sozinha (sem dose pendente, ou animal que já saiu) fica inativa e com a data em que foi
-- concluída. Fica nula nas desativadas pelo veterinário, que é como a tela as diferencia.
ALTER TABLE tb_prescricoes
    ADD COLUMN concluida_em DATETIME(6) NULL;
