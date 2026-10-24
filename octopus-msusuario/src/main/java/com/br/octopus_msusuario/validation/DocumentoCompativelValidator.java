package com.br.octopus_msusuario.validation;

import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DocumentoCompativelValidator implements ConstraintValidator<DocumentoCompativel, DadosDocumento> {

    @Override
    public boolean isValid(DadosDocumento dados, ConstraintValidatorContext contexto) {
        // Documento ou tipo ausente: quem avisa é o @NotBlank / @NotNull do próprio campo.
        if (dados == null || dados.cpfCnpj() == null || dados.cpfCnpj().isBlank() || dados.tipoPessoa() == null) {
            return true;
        }
        boolean fisica = dados.tipoPessoa() == TipoPessoa.PF;
        boolean valido = fisica ? Documentos.cpfValido(dados.cpfCnpj()) : Documentos.cnpjValido(dados.cpfCnpj());
        if (!valido) {
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(fisica
                            ? "CPF inválido: pessoa física precisa de um CPF com 11 dígitos, sem pontos nem traço"
                            : "CNPJ inválido: pessoa jurídica precisa de um CNPJ com 14 dígitos, sem pontos, barra nem traço")
                    .addPropertyNode("cpfCnpj")
                    .addConstraintViolation();
        }
        return valido;
    }
}
