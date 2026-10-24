package com.br.octopus_msprescricao.client;

import com.br.octopus_msprescricao.client.dto.AgendamentoDosesDto;
import com.br.octopus_msprescricao.client.dto.DoseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

// As doses moram no octopus-msdoses. Os dois endpoints exigem ROLE_VETERINARIO, que é quem prescreve, e são
// idempotentes lá: repetir o agendamento ou o cancelamento de uma prescrição não duplica nem muda nada.
@Component
public class DoseClient {

    private static final String SERVICO = "octopus-msdoses";
    private static final String NAO_ENCONTRADA = "Prescrição sem doses no octopus-msdoses";
    // Um agendamento grava até 200 doses por item; os 3 s dos outros clients são pouco para isso.
    private static final Duration LEITURA = Duration.ofSeconds(10);

    private final RestClient restClient;

    public DoseClient(@Value("${app.servicos.doses-url}") String baseUrl) {
        this.restClient = ClientesHttp.criar(baseUrl, LEITURA);
    }

    public void agendar(AgendamentoDosesDto agendamento) {
        ClientesHttp.chamar(() -> restClient.post()
                .uri("/api/doses/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(agendamento)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        ClientesHttp.erros(NAO_ENCONTRADA, SERVICO, agendamento.prescricaoId()))
                .toBodilessEntity(), SERVICO);
    }

    // As doses ainda pendentes viram CANCELADA; as aplicadas ficam como estão.
    public void cancelar(Long prescricaoId) {
        ClientesHttp.chamar(() -> restClient.patch()
                .uri("/api/doses/prescricoes/{id}/cancelar", prescricaoId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(NAO_ENCONTRADA, SERVICO, prescricaoId))
                .toBodilessEntity(), SERVICO);
    }

    // As doses canceladas que ainda estão na janela voltam a ficar pendentes e reservam o estoque de novo (422 se faltar
    // saldo). Devolve todas as doses da prescrição, para quem chamou ver o que voltou.
    public List<DoseDto> reativar(Long prescricaoId) {
        DoseDto[] doses = ClientesHttp.chamar(() -> restClient.patch()
                .uri("/api/doses/prescricoes/{id}/reativar", prescricaoId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientesHttp.erros(NAO_ENCONTRADA, SERVICO, prescricaoId))
                .body(DoseDto[].class), SERVICO);
        return doses == null ? List.of() : List.of(doses);
    }
}
