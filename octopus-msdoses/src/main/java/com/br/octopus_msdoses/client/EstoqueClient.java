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
import java.util.Map;

// Estoque do octopus-msmedications, em doses (1 dose = 1 unidade). Repassa o token de quem chamou: o veterinário
// que prescreveu (reservar e devolver) ou quem registrou a saída do animal (devolver). Mesmo padrão de client do
// msprescricao: HttpClient do JDK, porque o HttpURLConnection não faz PATCH.
@Slf4j
@Component
public class EstoqueClient {

    private static final String SERVICO = "octopus-msmedications";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EstoqueClient(@Value("${app.servicos.medicacoes-url}") String baseUrl, ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        var fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor(EstoqueClient::repassarToken)
                .build();
        this.objectMapper = objectMapper;
    }

    /** 422 se não houver saldo, com a mensagem do msmedications (nome do medicamento e quantidades). */
    public void reservar(Long medicacaoId, int quantidade) {
        movimentar(medicacaoId, quantidade, "reservar");
    }

    public void devolver(Long medicacaoId, int quantidade) {
        movimentar(medicacaoId, quantidade, "devolver");
    }

    private void movimentar(Long medicacaoId, int quantidade, String acao) {
        try {
            restClient.patch()
                    .uri("/api/medicacoes/{id}/estoque/{acao}", medicacaoId, acao)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantidade", quantidade))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                        int codigo = resposta.getStatusCode().value();
                        if (codigo == 422) {
                            throw new RegraDeNegocioException(mensagem(resposta));
                        }
                        if (codigo == 404) {
                            throw new RecursoNaoEncontradoException("Medicamento não encontrado: " + medicacaoId);
                        }
                        throw new ServicoIndisponivelException(SERVICO + " respondeu " + codigo + " ao " + acao
                                + " estoque do medicamento " + medicacaoId + ".");
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            log.warn("{} não respondeu: {}", SERVICO, e.getMessage());
            throw new ServicoIndisponivelException(SERVICO + " não respondeu.");
        }
    }

    private String mensagem(ClientHttpResponse resposta) throws IOException {
        JsonNode corpo = objectMapper.readTree(resposta.getBody());
        JsonNode mensagem = corpo == null ? null : corpo.get("mensagem");
        return mensagem == null ? "Estoque insuficiente." : mensagem.asString();
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
