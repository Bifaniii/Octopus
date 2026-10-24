package com.br.octopus_msusuario.validation;

/**
 * Dígito verificador de CPF e CNPJ. É aqui, e não no front, que a regra mora: o front só formata e confere o
 * tamanho, e o back recusa o número que não existe.
 */
public final class Documentos {

    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Documentos() {
    }

    /** 11 dígitos, sem máscara, não todos iguais, com os dois dígitos verificadores certos. */
    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || todosIguais(cpf)) {
            return false;
        }
        return digitoCpf(cpf, 9) == cpf.charAt(9) - '0' && digitoCpf(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** 14 dígitos, sem máscara, não todos iguais, com os dois dígitos verificadores certos. */
    public static boolean cnpjValido(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}") || todosIguais(cnpj)) {
            return false;
        }
        return digitoCnpj(cnpj, PESOS_CNPJ_1) == cnpj.charAt(12) - '0'
                && digitoCnpj(cnpj, PESOS_CNPJ_2) == cnpj.charAt(13) - '0';
    }

    private static boolean todosIguais(String digitos) {
        return digitos.chars().allMatch(c -> c == digitos.charAt(0));
    }

    private static int digitoCpf(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
