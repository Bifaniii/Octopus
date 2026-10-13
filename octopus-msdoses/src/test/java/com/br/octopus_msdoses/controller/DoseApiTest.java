package com.br.octopus_msdoses.controller;

import com.br.octopus_msdoses.client.EstoqueClient;
import com.br.octopus_msdoses.config.RelogioDeTeste;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import com.br.octopus_msdoses.exception.ServicoIndisponivelException;
import com.br.octopus_msdoses.security.TokensDeTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com H2 e exercita filtro JWT, @PreAuthorize e o fluxo das doses: agendamento vindo da
 * prescrição, aplicação pelo auxiliar e cancelamento. Ids de prescrição diferentes isolam um teste do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelogioDeTeste.Config.class)
class DoseApiTest {

    private static final String VET = "Bearer " + TokensDeTeste.gerar("vet@teste.local", "ROLE_VETERINARIO");
    private static final String AUXILIAR = "Bearer " + TokensDeTeste.gerar("paulo@teste.local", "ROLE_AUXILIAR");
    private static final String RECEPCAO = "Bearer " + TokensDeTeste.gerar("recepcao@teste.local", "ROLE_RECEPCIONISTA");
    private static final AtomicLong PROXIMA_PRESCRICAO = new AtomicLong(1000);

    @Autowired
    private MockMvc mockMvc;

    // Estoque do msmedications: aceita tudo por padrão (mock), e cada teste de estoque confere ou provoca falha.
    @MockitoBean
    private EstoqueClient estoqueClient;

    @Autowired
    private RelogioDeTeste relogio;

    // Na janela da primeira dose (08h) e antes de todas as outras: nenhuma é perdida durante estes testes.
    @BeforeEach
    void acertarRelogio() {
        relogio.acertar(LocalDateTime.of(2026, 10, 10, 7, 45));
    }

    private static String agendamento(long prescricaoId, long internacaoId) {
        return """
                {"prescricaoId": %d, "internacaoId": %d,
                 "itens": [{"itemPrescricaoId": %d, "medicacaoId": 1, "esquema": "CONTINUO",
                            "horarios": ["2026-10-10T08:00:00", "2026-10-10T16:00:00", "2026-10-11T00:00:00"]},
                           {"itemPrescricaoId": %d, "medicacaoId": 2, "esquema": "SINTOMATICO",
                            "horarios": ["2026-10-10T12:00:00"]}]}
                """.formatted(prescricaoId, internacaoId, prescricaoId * 10, prescricaoId * 10 + 1);
    }

    /** Agenda como veterinário e devolve os ids das doses, na ordem do horário. */
    private List<Integer> agendar(long prescricaoId, long internacaoId) throws Exception {
        String body = mockMvc.perform(post("/api/doses/agendamentos")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agendamento(prescricaoId, internacaoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/doses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void agendamentoCriaUmaDosePendentePorHorarioEmOrdem() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();

        mockMvc.perform(post("/api/doses/agendamentos")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agendamento(prescricao, 7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].status", everyItem(is("PENDENTE"))))
                .andExpect(jsonPath("$[0].horarioPrevisto").value("2026-10-10T08:00:00"))
                .andExpect(jsonPath("$[1].horarioPrevisto").value("2026-10-10T12:00:00"))
                .andExpect(jsonPath("$[1].medicacaoId").value(2));
    }

    @Test
    void agendarDeNovoAMesmaPrescricaoNaoDuplica() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();
        List<Integer> primeira = agendar(prescricao, 7);

        List<Integer> segunda = agendar(prescricao, 7);

        org.assertj.core.api.Assertions.assertThat(segunda).isEqualTo(primeira);
    }

    @Test
    void soVeterinarioAgendaECancela() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();

        mockMvc.perform(post("/api/doses/agendamentos")
                        .header("Authorization", AUXILIAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agendamento(prescricao, 7)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", RECEPCAO))
                .andExpect(status().isForbidden());
    }

    @Test
    void agendamentoSemItensRecebe400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/doses/agendamentos")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"internacaoId\": 7, \"itens\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.prescricaoId").exists())
                .andExpect(jsonPath("$.campos.itens").exists());
    }

    @Test
    void auxiliarAplicaEAAplicacaoGuardaQuemEQuando() throws Exception {
        Integer dose = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), 7).get(0);

        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose)
                        .header("Authorization", AUXILIAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\": \"Aceitou bem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APLICADA"))
                .andExpect(jsonPath("$.aplicadaPor").value("paulo@teste.local"))
                .andExpect(jsonPath("$.aplicadaEm").isNotEmpty())
                .andExpect(jsonPath("$.observacao").value("Aceitou bem"));
    }

    @Test
    void aplicarSemCorpoTambemFunciona() throws Exception {
        Integer dose = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), 7).get(0);

        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APLICADA"));
    }

    // Caso de teste 9 do enunciado.
    @Test
    void rn05_segundaAplicacaoDaMesmaDoseRecebe422() throws Exception {
        Integer dose = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), 7).get(0);
        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", AUXILIAR))
                .andExpect(status().isOk());

        String outro = "Bearer " + TokensDeTeste.gerar("outro@teste.local", "ROLE_AUXILIAR");
        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", outro))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("RN-05")));

        mockMvc.perform(get("/api/doses/{id}", dose).header("Authorization", VET))
                .andExpect(jsonPath("$.aplicadaPor").value("paulo@teste.local"));
    }

    @Test
    void veterinarioNaoRegistraAplicacao() throws Exception {
        Integer dose = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), 7).get(0);

        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", VET))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarDaPrescricaoCancelaAsPendentesEMantemAsAplicadas() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();
        Integer primeira = agendar(prescricao, 7).get(0);
        mockMvc.perform(patch("/api/doses/{id}/aplicar", primeira).header("Authorization", AUXILIAR))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APLICADA"))
                .andExpect(jsonPath("$[1].status").value("CANCELADA"))
                .andExpect(jsonPath("$[3].status").value("CANCELADA"));

        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", VET))
                .andExpect(status().isOk());
    }

    @Test
    void listarFiltraPorInternacaoStatusEPeriodo() throws Exception {
        long internacao = 5000 + PROXIMA_PRESCRICAO.incrementAndGet();
        Integer primeira = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao).get(0);
        agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao + 1);
        mockMvc.perform(patch("/api/doses/{id}/aplicar", primeira).header("Authorization", AUXILIAR))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/doses").param("internacaoId", String.valueOf(internacao))
                        .header("Authorization", RECEPCAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/api/doses").param("internacaoId", String.valueOf(internacao))
                        .param("status", "APLICADA").header("Authorization", AUXILIAR))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(primeira));
        mockMvc.perform(get("/api/doses").param("internacaoId", String.valueOf(internacao))
                        .param("de", "2026-10-11").param("ate", "2026-10-11").header("Authorization", AUXILIAR))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].horarioPrevisto").value("2026-10-11T00:00:00"));
    }

    @Test
    void doseInexistenteRecebe404() throws Exception {
        mockMvc.perform(get("/api/doses/999999").header("Authorization", VET))
                .andExpect(status().isNotFound());
    }

    @Test
    void agendarReservaUmaUnidadePorDoseDeCadaMedicamento() throws Exception {
        agendar(PROXIMA_PRESCRICAO.incrementAndGet(), 7);

        verify(estoqueClient).reservar(1L, 3);
        verify(estoqueClient).reservar(2L, 1);
    }

    @Test
    void semEstoqueOAgendamentoRecebe422ENadaEGravado() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();
        doThrow(new RegraDeNegocioException("Estoque insuficiente de Analgésico B: 0 em estoque, 1 pedidas."))
                .when(estoqueClient).reservar(eq(2L), anyInt());

        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON).content(agendamento(prescricao, 7)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente de Analgésico B: 0 em estoque, 1 pedidas."));

        // O medicamento 1 já tinha sido reservado e volta; nenhuma dose fica gravada.
        verify(estoqueClient).devolver(1L, 3);
        mockMvc.perform(get("/api/doses").param("prescricaoId", String.valueOf(prescricao)).header("Authorization", VET))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancelarDevolveAoEstoqueSoAsDosesQueEstavamPendentes() throws Exception {
        long prescricao = PROXIMA_PRESCRICAO.incrementAndGet();
        Integer primeira = agendar(prescricao, 7).get(0);
        mockMvc.perform(patch("/api/doses/{id}/aplicar", primeira).header("Authorization", AUXILIAR))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", VET))
                .andExpect(status().isOk());
        // Medicamento 1 tinha 3 doses, uma aplicada: voltam 2. Medicamento 2: volta 1.
        verify(estoqueClient).devolver(1L, 2);
        verify(estoqueClient).devolver(2L, 1);

        // Repetir não devolve de novo.
        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", VET))
                .andExpect(status().isOk());
        verify(estoqueClient, times(1)).devolver(1L, 2);
    }

    @Test
    void encerrarInternacaoCancelaAsPendentesDeTodasAsPrescricoes() throws Exception {
        long internacao = 9000 + PROXIMA_PRESCRICAO.incrementAndGet();
        agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao);
        agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao);
        Integer deOutraInternacao = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao + 1).get(0);

        mockMvc.perform(patch("/api/doses/internacoes/{id}/cancelar", internacao).header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/doses/internacoes/{id}/cancelar", internacao).header("Authorization", RECEPCAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[*].status", everyItem(is("CANCELADA"))));

        verify(estoqueClient).devolver(1L, 6);
        verify(estoqueClient).devolver(2L, 2);
        mockMvc.perform(get("/api/doses/{id}", deOutraInternacao).header("Authorization", VET))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void seOEstoqueNaoRespondeOCancelamentoEDesfeito() throws Exception {
        long internacao = 9000 + PROXIMA_PRESCRICAO.incrementAndGet();
        Integer dose = agendar(PROXIMA_PRESCRICAO.incrementAndGet(), internacao).get(0);
        doThrow(new ServicoIndisponivelException("octopus-msmedications não respondeu."))
                .when(estoqueClient).devolver(eq(2L), anyInt());

        mockMvc.perform(patch("/api/doses/internacoes/{id}/cancelar", internacao).header("Authorization", VET))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(get("/api/doses/{id}", dose).header("Authorization", VET))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
        // Uma reserva no agendamento e outra na compensação: o medicamento 1 já tinha sido devolvido.
        verify(estoqueClient, times(2)).reservar(1L, 3);
    }
}
