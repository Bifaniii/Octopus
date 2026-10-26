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

    // Manda o e-mail ao endereço cadastrado do tutor. Devolve o destinatário.
    public String enviarMensagemAoTutor(Long tutorId, String assunto, String texto) {
        java.util.Map<String, String> resposta = ClientesHttp.chamar(() -> restClient.post()
                .uri("/api/tutores/{id}/mensagens", tutorId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("assunto", assunto, "texto", texto))
                .retrieve()
                .onStatus(status -> status.value() == 422, (req, res) -> {
                    throw new com.br.octopus_msrelatorios.exception.RegraDeNegocioException(
                            "O tutor não tem e-mail cadastrado.");
                })
                .onStatus(status -> status.value() == 502, (req, res) -> {
                    throw new com.br.octopus_msrelatorios.exception.EmailNaoEnviadoException(
                            "O servidor de e-mail não aceitou o envio. Confira a conta de envio do sistema "
                                    + "(MAIL_USERNAME e MAIL_PASSWORD) e tente de novo.");
                })
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(SERVICO))
                .body(new ParameterizedTypeReference<java.util.Map<String, String>>() {
                }), SERVICO);
        return resposta == null ? null : resposta.get("destinatario");
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
