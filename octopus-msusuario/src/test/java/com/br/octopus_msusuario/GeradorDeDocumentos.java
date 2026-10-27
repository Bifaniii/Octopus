package com.br.octopus_msusuario;

import java.util.concurrent.ThreadLocalRandom;

/** CPFs e CNPJs com dígito verificador certo, para os testes cadastrarem usuários sem repetir número. */
public final class GeradorDeDocumentos {

    private GeradorDeDocumentos() {
    }

    public static String cpf() {
        while (true) {
            StringBuilder cpf = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                cpf.append(ThreadLocalRandom.current().nextInt(10));
            }
            cpf.append(digitoCpf(cpf, 10)).append(digitoCpf(cpf, 11));
            if (cpf.chars().distinct().count() > 1) {
                return cpf.toString();
            }
        }
    }

    public static String cnpj() {
        StringBuilder cnpj = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            cnpj.append(ThreadLocalRandom.current().nextInt(10));
        }
        cnpj.append("0001");
        cnpj.append(digitoCnpj(cnpj, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        cnpj.append(digitoCnpj(cnpj, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        return cnpj.toString();
    }

    /** CNPJ alfanumérico (formato de 2026): 8 caracteres aleatórios de 0-9 e A-Z, filial 0001 e os dois dígitos. */
    public static String cnpjAlfanumerico() {
        String alfabeto = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder cnpj = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            cnpj.append(alfabeto.charAt(ThreadLocalRandom.current().nextInt(alfabeto.length())));
        }
        cnpj.append("0001");
        cnpj.append(digitoCnpj(cnpj, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        cnpj.append(digitoCnpj(cnpj, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}));
        return cnpj.toString();
    }

    private static int digitoCpf(CharSequence base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int digitoCnpj(CharSequence base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
