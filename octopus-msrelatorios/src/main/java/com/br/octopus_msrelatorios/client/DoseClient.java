package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.DoseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

// Leitura liberada para qualquer perfil autenticado. O período compara a data do horário previsto.
@Component
public class DoseClient {

    private static final String SERVICO = "octopus-msdoses";

    private final RestClient restClient;

    public DoseClient(@Value("${app.servicos.doses-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public List<DoseDto> listarDaInternacao(Long internacaoId) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri(uri -> uri.path("/api/doses").queryParam("internacaoId", internacaoId).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<DoseDto>>() {
                }), SERVICO);
    }

    public List<DoseDto> listar(LocalDate de, LocalDate ate) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri(uri -> uri.path("/api/doses")
                        .queryParam("de", de)
                        .queryParam("ate", ate)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<DoseDto>>() {
                }), SERVICO);
    }
}
