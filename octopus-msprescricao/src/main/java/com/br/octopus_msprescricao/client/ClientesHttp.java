package com.br.octopus_msprescricao.client;

import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.ServicoIndisponivelException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

// Montagem e tratamento de erro comuns aos clients dos outros módulos (mesmo padrão do ms-internacao).
@Slf4j
final class ClientesHttp {

    private static final Duration CONEXAO = Duration.ofSeconds(2);
    private static final Duration LEITURA = Duration.ofSeconds(3);

    private ClientesHttp() {
    }

    // HttpClient do JDK, e não o HttpURLConnection do SimpleClientHttpRequestFactory: este recusa PATCH, que é o
    // verbo do iniciar-tratamento e do cancelamento das doses.
    static RestClient criar(String baseUrl) {
        return criar(baseUrl, LEITURA);
    }

    static RestClient criar(String baseUrl, Duration leitura) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONEXAO)
                .build();
        var fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(leitura);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor(ClientesHttp::repassarToken)
                .build();
    }

    // 404 vira RecursoNaoEncontrado (404 aqui também); qualquer outro erro vira ServicoIndisponivel (503), porque
    // não é culpa de quem chamou a prescrição.
    static RestClient.ResponseSpec.ErrorHandler erros(String naoEncontrado, String servico, Object id) {
        return (requisicao, resposta) -> {
            if (resposta.getStatusCode().value() == 404) {
                throw new RecursoNaoEncontradoException(naoEncontrado + ": " + id);
            }
            throw new ServicoIndisponivelException(servico + " respondeu " + resposta.getStatusCode().value()
                    + " para " + requisicao.getURI().getPath() + ".");
        };
    }

    // Sem resposta (timeout, conexão recusada): 503.
    static <T> T chamar(Supplier<T> chamada, String servico) {
        try {
            return chamada.get();
        } catch (ResourceAccessException e) {
            log.warn("{} não respondeu: {}", servico, e.getMessage());
            throw new ServicoIndisponivelException(servico + " não respondeu.");
        }
    }

    // Repassa o JWT de quem chamou: os outros módulos validam o mesmo token, com o mesmo JWT_SECRET.
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
