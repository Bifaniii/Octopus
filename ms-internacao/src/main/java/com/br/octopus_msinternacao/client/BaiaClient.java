package com.br.octopus_msinternacao.client;

import com.br.octopus_msinternacao.client.dto.BaiaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class BaiaClient {

    private final RestClient restClient;

    public BaiaClient(@Value("${app.servicos.baias-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public BaiaDto buscar(UUID id) {
        return ClientesHttp.buscar(restClient, "/api/baias/{id}", id, BaiaDto.class,
                "Baia não encontrada", "msbaias");
    }
}
