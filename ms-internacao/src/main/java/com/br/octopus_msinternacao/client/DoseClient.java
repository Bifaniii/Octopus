package com.br.octopus_msinternacao.client;

import com.br.octopus_msinternacao.exception.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// Doses no octopus-msdoses. O cancelamento é idempotente e devolve ao estoque as doses que estavam pendentes.
@Component
public class DoseClient {

    private static final String SERVICO = "octopus-msdoses";

    private final RestClient restClient;

    public DoseClient(@Value("${app.servicos.doses-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public void cancelarDaInternacao(Long internacaoId) {
        try {
            restClient.patch()
                    .uri("/api/doses/internacoes/{id}/cancelar", internacaoId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                        throw new ServicoIndisponivelException(SERVICO + " respondeu " + resposta.getStatusCode().value()
                                + " ao cancelar as doses da internação " + internacaoId + ".");
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException(SERVICO + " não respondeu ao cancelar as doses da internação "
                    + internacaoId + ".");
        }
    }
}
