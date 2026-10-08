
package com.br.octopus_msinternacao.client;

import com.br.octopus_msinternacao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msinternacao.exception.ServicoIndisponivelException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

// Montagem e tratamento de erro comuns aos clients dos outros módulos.
final class ClientesHttp {

    private static final Duration CONEXAO = Duration.ofSeconds(2);
    private static final Duration LEITURA = Duration.ofSeconds(3);

    private ClientesHttp() {
    }

    static RestClient criar(String baseUrl) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(CONEXAO);
        fabrica.setReadTimeout(LEITURA);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor(ClientesHttp::repassarToken)
                .build();
    }

    // GET de um recurso por id: 404 vira RecursoNaoEncontrado (404 aqui também); qualquer outro erro ou falta de
    // resposta vira ServicoIndisponivel (503), porque não é culpa de quem chamou a internação.
    static <T> T buscar(RestClient restClient, String caminho, UUID id, Class<T> tipo,
                        String naoEncontrado, String servico) {
        try {
            return restClient.get()
                    .uri(caminho, id)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                        if (resposta.getStatusCode().value() == 404) {
                            throw new RecursoNaoEncontradoException(naoEncontrado + ": " + id);
                        }
                        throw new ServicoIndisponivelException(
                                servico + " respondeu " + resposta.getStatusCode().value() + " ao consultar " + id + ".");
                    })
                    .body(tipo);
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException(servico + " não respondeu ao consultar " + id + ".");
        }
    }

    // Repassa o JWT de quem chamou a internação: os outros módulos validam o mesmo token, com o mesmo JWT_SECRET.
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
