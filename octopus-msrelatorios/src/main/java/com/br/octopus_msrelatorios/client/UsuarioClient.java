package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.client.dto.TutorDto;
import com.br.octopus_msrelatorios.client.dto.UsuarioDto;
import com.br.octopus_msrelatorios.client.dto.UsuarioNomeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class UsuarioClient {

    private static final String SERVICO = "octopus-msusuario";

    private final RestClient restClient;

    public UsuarioClient(@Value("${app.servicos.usuarios-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    // Qualquer perfil autenticado.
    public List<UsuarioNomeDto> nomes() {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/usuarios/nomes")
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<UsuarioNomeDto>>() {
                }), SERVICO);
    }

    // Só ADMIN. perfil é a ROLE_* e ativo pode ser nulo (todos).
    public List<UsuarioDto> usuarios(String perfil, Boolean ativo) {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri(uri -> uri.path("/api/usuarios")
                        .queryParamIfPresent("perfil", Optional.ofNullable(perfil))
                        .queryParamIfPresent("ativo", Optional.ofNullable(ativo))
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<UsuarioDto>>() {
                }), SERVICO);
    }

    // ADMIN, RECEPCIONISTA e VETERINARIO. Cada tutor já vem com os animais.
    public List<TutorDto> tutores() {
        return ClientesHttp.chamar(() -> restClient.get()
                .uri("/api/tutores")
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<List<TutorDto>>() {
                }), SERVICO);
    }
}
