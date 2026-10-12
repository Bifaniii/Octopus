package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.BaiaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Leitura liberada para qualquer perfil autenticado.
@Component
public class BaiaClient {

    private static final String SERVICO = "ms-cadastro-baias";

    private final RestClient restClient;

    public BaiaClient(@Value("${app.servicos.baias-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public List<BaiaDto> listar() {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/baias")
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<BaiaDto>>() {
                }), SERVICO);
    }
}
