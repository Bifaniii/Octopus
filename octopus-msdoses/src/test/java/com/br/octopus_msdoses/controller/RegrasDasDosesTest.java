package com.br.octopus_msdoses.controller;

import com.br.octopus_msdoses.client.EstoqueClient;
import com.br.octopus_msdoses.config.RelogioDeTeste;
import com.br.octopus_msdoses.security.TokensDeTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Janela de aplicação (RN-03), dose perdida (RN-04), reaplicação por esquema (RN-07), painel (tela 6) e item "se
 * necessário" (mudança contratada de 01/11), com o relógio acertado como nos casos de teste do enunciado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelogioDeTeste.Config.class)
class RegrasDasDosesTest {

    private static final String VET = "Bearer " + TokensDeTeste.gerar("vet@teste.local", "ROLE_VETERINARIO");
    private static final String AUXILIAR = "Bearer " + TokensDeTeste.gerar("paulo@teste.local", "ROLE_AUXILIAR");
    private static final String ADMIN = "Bearer " + TokensDeTeste.gerar("admin@teste.local", "ROLE_ADMIN");
    private static final AtomicLong PROXIMO = new AtomicLong(50_000);
    private static final LocalDateTime AS_14H = LocalDateTime.of(2026, 11, 3, 14, 0);
    private static final long ANTIBIOTICO_A = 11, ANALGESICO_B = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioDeTeste relogio;

    @MockitoBean
    private EstoqueClient estoqueClient;

    /** Prescrição de um item com horário fixo; devolve o id da prescrição. Agenda com o relógio às 13h. */
    private long prescrever(long internacao, long medicacao, String esquema, String... horarios) throws Exception {
        relogio.acertar(AS_14H.minusHours(1));
        long prescricao = PROXIMO.incrementAndGet();
        String lista = String.join("\", \"", horarios);
        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prescricaoId": %d, "internacaoId": %d,
                                 "itens": [{"itemPrescricaoId": %d, "medicacaoId": %d, "esquema": "%s",
                                            "horarios": ["%s"]}]}
                                """.formatted(prescricao, internacao, prescricao * 10, medicacao, esquema, lista)))
                .andExpect(status().isCreated());
        return prescricao;
    }

    private ResultActions doses(long prescricao) throws Exception {
        return mockMvc.perform(get("/api/doses").param("prescricaoId", String.valueOf(prescricao))
                .header("Authorization", VET));
    }

    private int primeiraDose(long prescricao) throws Exception {
        String body = doses(prescricao).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[*].id");
        return ids.get(0);
    }

    // Caso de teste 5: dose prevista para 14h, agora são 14h20.
    @Test
    void caso5_aplicacaoAs14h20DaDoseDas14hEAceita() throws Exception {
        long prescricao = prescrever(PROXIMO.incrementAndGet(), ANTIBIOTICO_A, "CONTINUO", "2026-11-03T14:00:00");
        int dose = primeiraDose(prescricao);

        relogio.acertar(AS_14H.plusMinutes(20));
        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APLICADA"))
                .andExpect(jsonPath("$.aplicadaEm").value("2026-11-03T14:20:00"));
    }

    @Test
    void rn03_aplicacaoAntesDaJanelaRecebe422() throws Exception {
        long prescricao = prescrever(PROXIMO.incrementAndGet(), ANTIBIOTICO_A, "CONTINUO", "2026-11-03T14:00:00");
        int dose = primeiraDose(prescricao);

        relogio.acertar(AS_14H.minusMinutes(40));
        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", AUXILIAR))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("RN-03")));
    }

    // Caso de teste 6: dose prevista para 14h, agora são 14h50.
    @Test
    void caso6_as14h50ADoseDas14hConstaComoPerdidaNoPainel() throws Exception {
        long internacao = PROXIMO.incrementAndGet();
        long prescricao = prescrever(internacao, ANALGESICO_B, "SINTOMATICO", "2026-11-03T14:00:00");
        int dose = primeiraDose(prescricao);

        relogio.acertar(AS_14H.plusMinutes(50));
        mockMvc.perform(get("/api/doses/painel").param("internacaoId", String.valueOf(internacao))
                        .header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perdidas[*].id", hasItem(dose)))
                .andExpect(jsonPath("$.atrasadas.length()").value(0));
        mockMvc.perform(patch("/api/doses/{id}/aplicar", dose).header("Authorization", AUXILIAR))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("RN-04")));
    }

    // Caso de teste 7: dose de Antibiótico A (contínuo) perdida; o esquema inteiro é deslocado.
    @Test
    void caso7_perdidaDeContinuoEReaplicadaEDeslocaOResto() throws Exception {
        long prescricao = prescrever(PROXIMO.incrementAndGet(), ANTIBIOTICO_A, "CONTINUO",
                "2026-11-03T14:00:00", "2026-11-03T22:00:00", "2026-11-04T06:00:00");
        int perdida = primeiraDose(prescricao);

        relogio.acertar(AS_14H.plusMinutes(50));
        doses(prescricao)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].status").value("PERDIDA"))
                .andExpect(jsonPath("$[1].horarioPrevisto").value("2026-11-03T14:50:00"))
                .andExpect(jsonPath("$[1].status").value("PENDENTE"))
                .andExpect(jsonPath("$[1].reaplicacaoDe").value(perdida))
                .andExpect(jsonPath("$[2].horarioPrevisto").value("2026-11-03T22:50:00"))
                .andExpect(jsonPath("$[3].horarioPrevisto").value("2026-11-04T06:50:00"));
        // A reaplicação usa a unidade já reservada da perdida.
        verify(estoqueClient, never()).devolver(org.mockito.ArgumentMatchers.eq(ANTIBIOTICO_A),
                org.mockito.ArgumentMatchers.anyInt());

        // A reaplicação também tem janela, e pode ser aplicada.
        String body = doses(prescricao).andReturn().getResponse().getContentAsString();
        int reaplicacao = JsonPath.<List<Integer>>read(body, "$[*].id").get(1);
        relogio.acertar(AS_14H.plusMinutes(55));
        mockMvc.perform(patch("/api/doses/{id}/aplicar", reaplicacao).header("Authorization", AUXILIAR))
                .andExpect(status().isOk());
    }

    @Test
    void rn07_reaplicacaoPerdidaTambemEReaplicada() throws Exception {
        long prescricao = prescrever(PROXIMO.incrementAndGet(), ANTIBIOTICO_A, "CONTINUO",
                "2026-11-03T14:00:00", "2026-11-03T22:00:00");
        relogio.acertar(AS_14H.plusMinutes(50));
        doses(prescricao).andExpect(jsonPath("$.length()").value(3));

        relogio.acertar(AS_14H.plusMinutes(85));
        doses(prescricao)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[1].status").value("PERDIDA"))
                .andExpect(jsonPath("$[2].horarioPrevisto").value("2026-11-03T15:25:00"))
                .andExpect(jsonPath("$[3].horarioPrevisto").value("2026-11-03T23:25:00"));
    }

    // Caso de teste 8: dose de Analgésico B (sintomático) perdida; os horários seguintes ficam como estão.
    @Test
    void caso8_perdidaDeSintomaticoEPuladaEDevolveAUnidade() throws Exception {
        long prescricao = prescrever(PROXIMO.incrementAndGet(), ANALGESICO_B, "SINTOMATICO",
                "2026-11-03T14:00:00", "2026-11-03T22:00:00");

        relogio.acertar(AS_14H.plusMinutes(50));
        doses(prescricao)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PERDIDA"))
                .andExpect(jsonPath("$[1].horarioPrevisto").value("2026-11-03T22:00:00"))
                .andExpect(jsonPath("$[1].status").value("PENDENTE"));
        verify(estoqueClient).devolver(ANALGESICO_B, 1);
    }

    @Test
    void painelSeparaAtrasadasDasQueVencemNaProximaHora() throws Exception {
        long internacao = PROXIMO.incrementAndGet();
        prescrever(internacao, ANALGESICO_B, "SINTOMATICO",
                "2026-11-03T14:00:00", "2026-11-03T14:40:00", "2026-11-03T16:00:00");

        relogio.acertar(AS_14H.plusMinutes(10));
        mockMvc.perform(get("/api/doses/painel").param("internacaoId", String.valueOf(internacao))
                        .header("Authorization", ADMIN))
                .andExpect(jsonPath("$.agora").value("2026-11-03T14:10:00"))
                .andExpect(jsonPath("$.atrasadas.length()").value(1))
                .andExpect(jsonPath("$.atrasadas[0].horarioPrevisto").value("2026-11-03T14:00:00"))
                .andExpect(jsonPath("$.proximas.length()").value(1))
                .andExpect(jsonPath("$.proximas[0].horarioPrevisto").value("2026-11-03T14:40:00"))
                .andExpect(jsonPath("$.perdidas.length()").value(0));
    }

    private long prescreverSeNecessario(long internacao, int intervalo) throws Exception {
        long prescricao = PROXIMO.incrementAndGet();
        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prescricaoId": %d, "internacaoId": %d,
                                 "itens": [{"itemPrescricaoId": %d, "medicacaoId": %d, "esquema": "SINTOMATICO",
                                            "seNecessario": true, "intervaloMinimoHoras": %d}]}
                                """.formatted(prescricao, internacao, prescricao * 10, ANALGESICO_B, intervalo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(0));
        return prescricao * 10;
    }

    private ResultActions aplicarSeNecessario(long item) throws Exception {
        return mockMvc.perform(post("/api/doses/se-necessario/{item}/aplicacoes", item)
                .header("Authorization", AUXILIAR));
    }

    // Mudança contratada de 01/11: sem horário fixo, fora do painel de atrasos, com intervalo mínimo.
    @Test
    void seNecessarioRespeitaOIntervaloMinimoEFicaForaDosAtrasos() throws Exception {
        long internacao = PROXIMO.incrementAndGet();
        long item = prescreverSeNecessario(internacao, 6);

        relogio.acertar(AS_14H);
        aplicarSeNecessario(item)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APLICADA"))
                .andExpect(jsonPath("$.seNecessario").value(true))
                .andExpect(jsonPath("$.aplicadaPor").value("paulo@teste.local"));

        relogio.acertar(AS_14H.plusHours(3));
        aplicarSeNecessario(item)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("20:00")));
        mockMvc.perform(get("/api/doses/painel").param("internacaoId", String.valueOf(internacao))
                        .header("Authorization", AUXILIAR))
                .andExpect(jsonPath("$.atrasadas.length()").value(0))
                .andExpect(jsonPath("$.perdidas.length()").value(0))
                .andExpect(jsonPath("$.seNecessario[0].liberadoEm").value("2026-11-03T20:00:00"))
                .andExpect(jsonPath("$.seNecessario[0].liberado").value(false));

        relogio.acertar(AS_14H.plusHours(6));
        aplicarSeNecessario(item).andExpect(status().isCreated());
        // A unidade é reservada a cada aplicação aceita; a recusada nem chega a reservar.
        verify(estoqueClient, times(2)).reservar(ANALGESICO_B, 1);
    }

    @Test
    void seNecessarioDeprescricaoDesativadaNaoAplica() throws Exception {
        long item = prescreverSeNecessario(PROXIMO.incrementAndGet(), 4);
        long prescricao = item / 10;

        mockMvc.perform(patch("/api/doses/prescricoes/{id}/cancelar", prescricao).header("Authorization", VET))
                .andExpect(status().isOk());
        aplicarSeNecessario(item)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("encerrada")));
    }

    @Test
    void soAuxiliarAplicaSeNecessario() throws Exception {
        long item = prescreverSeNecessario(PROXIMO.incrementAndGet(), 4);

        mockMvc.perform(post("/api/doses/se-necessario/{item}/aplicacoes", item).header("Authorization", VET))
                .andExpect(status().isForbidden());
    }

    @Test
    void agendamentoIncoerenteRecebe422() throws Exception {
        String semIntervalo = """
                {"prescricaoId": %d, "internacaoId": 1,
                 "itens": [{"itemPrescricaoId": 1, "medicacaoId": 1, "esquema": "CONTINUO", "seNecessario": true}]}
                """.formatted(PROXIMO.incrementAndGet());
        String semHorarios = """
                {"prescricaoId": %d, "internacaoId": 1,
                 "itens": [{"itemPrescricaoId": 2, "medicacaoId": 1, "esquema": "CONTINUO"}]}
                """.formatted(PROXIMO.incrementAndGet());
        String semEsquema = """
                {"prescricaoId": %d, "internacaoId": 1,
                 "itens": [{"itemPrescricaoId": 3, "medicacaoId": 1, "horarios": ["2026-11-03T14:00:00"]}]}
                """.formatted(PROXIMO.incrementAndGet());

        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON).content(semIntervalo))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON).content(semHorarios))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/doses/agendamentos").header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON).content(semEsquema))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", not(org.hamcrest.Matchers.anEmptyMap())));
    }
}
