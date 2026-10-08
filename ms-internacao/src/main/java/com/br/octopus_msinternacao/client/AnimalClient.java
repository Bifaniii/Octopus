package com.br.octopus_msinternacao.client;

import com.br.octopus_msinternacao.client.dto.AnimalDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AnimalClient {

    private final RestClient restClient;

    public AnimalClient(@Value("${app.servicos.usuario-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public AnimalDto buscar(UUID id) {
        return ClientesHttp.buscar(restClient, "/api/animais/{id}", id, AnimalDto.class,
                "Animal não encontrado", "msusuario");
    }
}
