package com.br.octopus_msprescricao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor da conclusão automática das prescrições, que roda depois de a listagem responder. Uma thread e uma vaga
 * na fila: com uma conferência rodando e outra esperando, as próximas são descartadas, porque a que está na fila já
 * vai ver o mesmo estado. Assim, com o ms-internacao ou o msdoses lentos, as conferências não se acumulam na memória
 * nem disputam as conexões do banco.
 */
@Configuration
public class ConclusaoConfig {

    public static final String EXECUTOR = "conclusaoDePrescricoes";

    @Bean(name = EXECUTOR, destroyMethod = "shutdown")
    public ThreadPoolExecutor conclusaoDePrescricoes() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());
        executor.setThreadFactory(tarefa -> {
            Thread thread = new Thread(tarefa, "conclusao-prescricoes");
            thread.setDaemon(true);
            return thread;
        });
        return executor;
    }
}
