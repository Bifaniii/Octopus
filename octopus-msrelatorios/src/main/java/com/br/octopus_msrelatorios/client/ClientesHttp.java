package com.br.octopus_msrelatorios.client;

import com.br.octopus_msrelatorios.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

// Mesmo padrão dos clients do ms-internacao e do msprescricao: HttpClient do JDK e o token de quem pediu o
// relatório repassado adiante, então cada módulo aplica as próprias permissões sobre os dados que devolve.
@Slf4j
final class ClientesHttp {

    private static final Duration CONEXAO = Duration.ofSeconds(2);
    // Relatório lê listas inteiras; 3 s, como nos outros clients, é pouco para um período longo de doses.
    private static final Duration LEITURA = Duration.ofSeconds(10);

    private ClientesHttp() {
    }

    static RestClient criar(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONEXAO)
                .build();
        var fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(LEITURA);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor(ClientesHttp::repassarToken)
                .build();
    }

    // Qualquer erro do outro lado vira 503: o relatório não sai incompleto.
    static RestClient.ResponseSpec.ErrorHandler erros(String servico) {
        return (requisicao, resposta) -> {
            throw new ServicoIndisponivelException(servico + " respondeu " + resposta.getStatusCode().value()
                    + " para " + requisicao.getURI().getPath() + ".");
        };
    }

    static <T> T chamar(Supplier<T> chamada, String servico) {
        try {
            return chamada.get();
        } catch (ResourceAccessException e) {
            log.warn("{} não respondeu: {}", servico, e.getMessage());
            throw new ServicoIndisponivelException(servico + " não respondeu.");
        }
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
