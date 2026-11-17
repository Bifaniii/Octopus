package com.br.octopus_msprescricao.client;

import java.util.function.Supplier;

/**
 * Token de quem pediu, para chamadas aos outros módulos feitas fora da thread da requisição. Os clients repassam o
 * Authorization da requisição em andamento; numa tarefa em segundo plano essa requisição já terminou (e o objeto
 * dela pode ter sido reaproveitado pelo Tomcat), então o token é guardado antes, como texto, e devolvido aqui.
 */
public final class TokenRepassado {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private TokenRepassado() {
    }

    /** O Authorization da requisição em andamento, ou null fora de uma requisição. */
    public static String daRequisicaoAtual() {
        return ClientesHttp.tokenDaRequisicao();
    }

    /** Roda a tarefa com o token disponível para os clients, e limpa ao terminar. */
    public static <T> T executarCom(String token, Supplier<T> tarefa) {
        TOKEN.set(token);
        try {
            return tarefa.get();
        } finally {
            TOKEN.remove();
        }
    }

    static String daTarefaAtual() {
        return TOKEN.get();
    }
}
