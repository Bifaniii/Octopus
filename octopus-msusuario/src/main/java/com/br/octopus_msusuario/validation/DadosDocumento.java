package com.br.octopus_msusuario.validation;

import com.br.octopus_msusuario.domain.enums.TipoPessoa;

/** O que a validação do documento do veterinário precisa ler: o número e o tipo de pessoa. */
public interface DadosDocumento {

    String cpfCnpj();

    TipoPessoa tipoPessoa();
}
