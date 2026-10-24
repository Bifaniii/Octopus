package com.br.octopus_msinternacao.service;

import com.br.octopus_msinternacao.client.RelatoriosClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manda o resumo de alta ao tutor depois que a alta foi gravada. Espera o commit (o msrelatorios lê a internação por
 * HTTP e só enxerga o que já foi confirmado) e roda em outra thread, para o envio do e-mail não atrasar a resposta.
 */
@Component
public class ResumoDeAltaNotificador {

    private final RelatoriosClient relatoriosClient;
    private final ExecutorService executor;

    @Autowired
    public ResumoDeAltaNotificador(RelatoriosClient relatoriosClient) {
        this(relatoriosClient, Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "resumo-de-alta");
            thread.setDaemon(true);
            return thread;
        }));
    }

    ResumoDeAltaNotificador(RelatoriosClient relatoriosClient, ExecutorService executor) {
        this.relatoriosClient = relatoriosClient;
        this.executor = executor;
    }

    public void aposOCommit(Long internacaoId) {
        String token = tokenDaRequisicao();
        if (token == null) {
            return; // sem token não há como autenticar no msrelatorios
        }
        Runnable envio = () -> executor.execute(() -> relatoriosClient.enviarResumoDeAlta(internacaoId, token));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private static String tokenDaRequisicao() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
