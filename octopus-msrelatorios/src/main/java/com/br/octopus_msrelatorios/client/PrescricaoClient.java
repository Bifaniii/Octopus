package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.PrescricaoDto;
import com.br.octopus_msrelatorios.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Leitura liberada para qualquer perfil autenticado.
@Component
public class PrescricaoClient {

    private static final String SERVICO = "octopus-msprescricao";

    private final RestClient restClient;

    public PrescricaoClient(@Value("${app.servicos.prescricoes-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public List<PrescricaoDto> listarDaInternacao(Long internacaoId) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri(uri -> uri.path("/api/prescricoes").queryParam("internacaoId", internacaoId).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<PrescricaoDto>>() {
                }), SERVICO);
    }

    public PrescricaoDto buscar(Long id) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/prescricoes/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new RecursoNaoEncontradoException("Prescrição não encontrada: " + id);
                })
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(PrescricaoDto.class), SERVICO);
    }
}
