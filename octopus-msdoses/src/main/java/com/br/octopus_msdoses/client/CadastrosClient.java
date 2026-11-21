package com.br.octopus_msdoses.client;

import com.br.octopus_msdoses.exception.RecursoNaoEncontradoException;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import com.br.octopus_msdoses.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Leituras e a única escrita que a aplicação da antirrábica precisa nos outros módulos: o nome do medicamento
 * (octopus-msmedications), o animal da internação (ms-internacao) e a data da vacina no cadastro do animal
 * (octopus-msusuario). Repassa o token de quem aplicou. Mesmo padrão do EstoqueClient: HttpClient do JDK, timeouts
 * curtos, 404 vira 404, 422 vira 422 com a mensagem do outro módulo e o resto vira 503.
 */
@Slf4j
@Component
public class CadastrosClient {

    private final RestClient medicacoes;
    private final RestClient internacoes;
    private final RestClient usuarios;
    private final ObjectMapper objectMapper;

    public CadastrosClient(@Value("${app.servicos.medicacoes-url}") String medicacoesUrl,
                           @Value("${app.servicos.internacao-url}") String internacaoUrl,
                           @Value("${app.servicos.usuario-url}") String usuarioUrl,
                           ObjectMapper objectMapper) {
        this.medicacoes = criar(medicacoesUrl);
        this.internacoes = criar(internacaoUrl);
        this.usuarios = criar(usuarioUrl);
        this.objectMapper = objectMapper;
    }

    /** Nome comercial do medicamento, como está no cadastro. */
    public String nomeDoMedicamento(Long medicacaoId) {
        JsonNode corpo = chamar("octopus-msmedications", "Medicamento não encontrado: " + medicacaoId,
                () -> medicacoes.get().uri("/api/medicacoes/{id}", medicacaoId));
        return texto(corpo, "nomeComercial");
    }

    /** Id do animal internado. */
    public Long animalDaInternacao(Long internacaoId) {
        JsonNode corpo = chamar("ms-internacao", "Internação não encontrada: " + internacaoId,
                () -> internacoes.get().uri("/api/internacoes/{id}", internacaoId));
        JsonNode animal = corpo == null ? null : corpo.get("animalId");
        if (animal == null || animal.isNull()) {
            throw new ServicoIndisponivelException("ms-internacao não informou o animal da internação " + internacaoId + ".");
        }
        return animal.asLong();
    }

    /** Grava a data da última antirrábica no cadastro do animal (o msusuario só avança a data, nunca volta). */
    public void registrarAntirrabica(Long animalId, LocalDate data) {
        chamar("octopus-msusuario", "Animal não encontrado: " + animalId,
                () -> usuarios.patch().uri("/api/animais/{id}/antirrabica", animalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("data", data.toString())));
    }

    private JsonNode chamar(String servico, String naoEncontrado, Supplier<RestClient.RequestHeadersSpec<?>> requisicao) {
        try {
            return requisicao.get()
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (pedido, resposta) -> {
                        int codigo = resposta.getStatusCode().value();
                        if (codigo == 404) {
                            throw new RecursoNaoEncontradoException(naoEncontrado);
                        }
                        if (codigo == 422) {
                            throw new RegraDeNegocioException(servico + " recusou: " + mensagem(resposta));
                        }
                        throw new ServicoIndisponivelException(servico + " respondeu " + codigo + " para "
                                + pedido.getURI().getPath() + ".");
                    })
                    .body(JsonNode.class);
        } catch (ResourceAccessException e) {
            log.warn("{} não respondeu: {}", servico, e.getMessage());
            throw new ServicoIndisponivelException(servico + " não respondeu.");
        }
    }

    private String mensagem(ClientHttpResponse resposta) throws IOException {
        JsonNode corpo = objectMapper.readTree(resposta.getBody());
        JsonNode mensagem = corpo == null ? null : corpo.get("mensagem");
        return mensagem == null ? "operação recusada" : mensagem.asString();
    }

    private static String texto(JsonNode corpo, String campo) {
        JsonNode valor = corpo == null ? null : corpo.get(campo);
        return valor == null || valor.isNull() ? "" : valor.asString();
    }

    private static RestClient criar(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        var fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor(CadastrosClient::repassarToken)
                .build();
    }

    private static ClientHttpResponse repassarToken(HttpRequest request, byte[] body,
                                                    ClientHttpRequestExecution execution) throws IOException {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String token = atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (token != null) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, token);
            }
        }
        return execution.execute(request, body);
    }
}
