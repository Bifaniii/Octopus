package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.MedicacaoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Leitura liberada para qualquer perfil autenticado. Inclui os medicamentos desativados.
@Component
public class MedicacaoClient {

    private static final String SERVICO = "octopus-msmedications";

    private final RestClient restClient;

    public MedicacaoClient(@Value("${app.servicos.medicacoes-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public List<MedicacaoDto> listar() {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/medicacoes")
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<MedicacaoDto>>() {
                }), SERVICO);
    }
}
