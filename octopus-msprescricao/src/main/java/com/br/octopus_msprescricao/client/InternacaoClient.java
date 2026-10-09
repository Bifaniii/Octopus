package com.br.octopus_msprescricao.client;

import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InternacaoClient {

    private static final String SERVICO = "ms-internacao";
    private static final String NAO_ENCONTRADA = "Internação não encontrada";

    private final RestClient restClient;

    public InternacaoClient(@Value("${app.servicos.internacao-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public InternacaoDto buscar(Long id) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/internacoes/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(NAO_ENCONTRADA, SERVICO, id))
                .body(InternacaoDto.class), SERVICO);
    }

    // Admitida → Em tratamento (a guarda dessa transição é "prescrição ativa"). Idempotente no ms-internacao:
    // nas prescrições seguintes, ou com a internação já em tratamento ou isolamento, não muda nada.
    // Exige ROLE_VETERINARIO, que é quem prescreve.
    public void iniciarTratamento(Long id) {
        ClientesHttp.chamar(() -> restClient.patch()
                .uri("/api/internacoes/{id}/iniciar-tratamento", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(NAO_ENCONTRADA, SERVICO, id))
                .toBodilessEntity(), SERVICO);
    }
}
