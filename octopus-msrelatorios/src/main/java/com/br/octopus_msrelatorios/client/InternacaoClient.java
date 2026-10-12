package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Leitura liberada para qualquer perfil autenticado.
@Component
public class InternacaoClient {

    private static final String SERVICO = "ms-internacao";

    private final RestClient restClient;

    public InternacaoClient(@Value("${app.servicos.internacoes-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public List<InternacaoDto> listar() {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/internacoes")
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<InternacaoDto>>() {
                }), SERVICO);
    }
}
