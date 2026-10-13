package com.br.octopus_msinternacao.client;

import com.br.octopus_msinternacao.exception.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

// Prescrições no octopus-msprescricao. Guarda da transição Admitida → Em tratamento: "não sem prescrição ativa".
@Component
public class PrescricaoClient {

    private static final String SERVICO = "octopus-msprescricao";

    private final RestClient restClient;

    // Só o que a guarda usa da PrescricaoResponse; o resto do JSON é ignorado.
    record PrescricaoDto(Long id, boolean ativo) {
    }

    public PrescricaoClient(@Value("${app.servicos.prescricoes-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl);
    }

    public boolean temPrescricaoAtiva(Long internacaoId) {
        try {
            PrescricaoDto[] prescricoes = restClient.get()
                    .uri("/api/prescricoes?internacaoId={id}", internacaoId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                        throw new ServicoIndisponivelException(SERVICO + " respondeu "
                                + resposta.getStatusCode().value() + " ao consultar as prescrições da internação "
                                + internacaoId + ".");
                    })
                    .body(PrescricaoDto[].class);
            return prescricoes != null && Arrays.stream(prescricoes).anyMatch(PrescricaoDto::ativo);
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException(SERVICO + " não respondeu ao consultar as prescrições da internação "
                    + internacaoId + ".");
        }
    }
}
