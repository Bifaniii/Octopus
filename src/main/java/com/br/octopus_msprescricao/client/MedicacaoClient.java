package com.br.octopus_msprescricao.client;

import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;



@Component
public class MedicacaoClient {

    private static final String SERVICO = "octopus-msmedications";

    private final RestClient restClient;

    public MedicacaoClient(@Value("${app.servicos.medicacoes-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    // Leitura liberada para qualquer perfil autenticado no msmedications.
    public MedicacaoDto buscar(Long id) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/medicacoes/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros("Medicamento não encontrado", SERVICO, id))
                .body(MedicacaoDto.class), SERVICO);
    }
}
