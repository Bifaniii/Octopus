-- E-mail do tutor, opcional: recebe o resumo de alta do animal. Tutor sem e-mail segue como antes.
ALTER TABLE tb_tutores
    ADD COLUMN email VARCHAR(150) NULL;
