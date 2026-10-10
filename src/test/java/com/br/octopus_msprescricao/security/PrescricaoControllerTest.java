package com.br.octopus_msprescricao.security;

import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.exception.ServicoIndisponivelException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com H2 (ver src/test/resources/application.properties) e exercita o filtro JWT,
 * o @PreAuthorize e o fluxo de prescrição de ponta a ponta, com o ms-internacao mockado. Os tokens são emitidos
 * por TokensDeTeste, como o msusuario faria. Ids de internação diferentes isolam um teste do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrescricaoControllerTest {

    private static final String VET = TokensDeTeste.bearer("vet@teste.local", "ROLE_VETERINARIO");
    private static final String AUXILIAR = TokensDeTeste.bearer("aux@teste.local", "ROLE_AUXILIAR");
    private static final String ADMIN = TokensDeTeste.bearer("admin@teste.local", "ROLE_ADMIN");
    private static final AtomicLong PROXIMA_INTERNACAO = new AtomicLong(1000);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternacaoClient internacaoClient;

    @MockitoBean
    private MedicacaoClient medicacaoClient;

    @BeforeEach
    void internacoesAbertas() {
        // Qualquer internação consultada existe e está admitida, salvo quando o teste diz o contrário.
        when(internacaoClient.buscar(anyLong()))
                .thenAnswer(inv -> new InternacaoDto(inv.getArgument(0), "ADMITIDA", "Thor"));
        // E qualquer medicamento existe, sem interação proibida.
        when(medicacaoClient.buscar(any(Long.class)))
                .thenAnswer(inv -> new MedicacaoDto(inv.getArgument(0), "Medicamento", true, List.of()));
    }

    private static long novaInternacao() {
        return PROXIMA_INTERNACAO.incrementAndGet();
    }

    private static long randomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    private static String corpoValido(long internacaoId) {
        return """
                {"internacaoId": %d, "observacao": "Animal agitado",
                 "itens": [{"medicacaoId": "%s", "intervaloHoras": 8, "inicio": "2026-10-10T08:00:00"}]}
                """.formatted(internacaoId, randomLong());
    }

    /** Cria uma prescrição como veterinário e devolve o id. */
    private String criarPrescricao(long internacaoId) throws Exception {
        String body = mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(internacaoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Object) JsonPath.read(body, "$.id"));
    }

    @Test
    void semTokenRecebe401EmJson() throws Exception {
        mockMvc.perform(get("/api/prescricoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoRecebe401() throws Exception {
        mockMvc.perform(get("/api/prescricoes").header("Authorization", "Bearer lixo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auxiliarNaoPodePrescreverMasPodeLer() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", AUXILIAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(novaInternacao())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/prescricoes").header("Authorization", AUXILIAR))
                .andExpect(status().isOk());
    }

    @Test
    void adminNaoPrescreve() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(novaInternacao())))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinarioPrescreve() throws Exception {
        long internacaoId = novaInternacao();
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(internacaoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.internacaoId").value(internacaoId))
                .andExpect(jsonPath("$.veterinarioEmail").value("vet@teste.local"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].intervaloHoras").value(8));
    }

    @Test
    void internacaoEncerradaRecebe422() throws Exception {
        long internacaoId = novaInternacao();
        when(internacaoClient.buscar(internacaoId)).thenReturn(new InternacaoDto(internacaoId, "ENCERRADA", "Thor"));

        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(internacaoId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("ENCERRADA")));
    }

    @Test
    void interacaoProibidaRecebe422ComARegra() throws Exception {
        Long meloxicam = randomLong();
        Long cetoprofeno = randomLong();
        when(medicacaoClient.buscar(meloxicam)).thenReturn(new MedicacaoDto(meloxicam, "Meloxicam", true,
                List.of(new MedicacaoDto.Interacao(cetoprofeno, "Cetoprofeno"))));

        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"internacaoId": %d,
                                 "itens": [{"medicacaoId": "%s", "intervaloHoras": 24, "inicio": "2026-10-10T08:00:00"},
                                           {"medicacaoId": "%s", "intervaloHoras": 24, "inicio": "2026-10-10T08:00:00"}]}
                                """.formatted(novaInternacao(), meloxicam, cetoprofeno)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("RN-03")));
    }

    @Test
    void internacaoForaDoArRecebe503() throws Exception {
        long internacaoId = novaInternacao();
        when(internacaoClient.buscar(internacaoId)).thenThrow(new ServicoIndisponivelException("ms-internacao não respondeu."));

        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(internacaoId)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("ms-internacao não respondeu."));
    }

    @Test
    void corpoSemInternacaoENemItensRecebe400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.internacaoId").exists())
                .andExpect(jsonPath("$.campos.itens").exists());
    }

    @Test
    void intervaloZeroRecebe400ApontandoOCampoDoItem() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"internacaoId": %d,
                                 "itens": [{"medicacaoId": "%s", "intervaloHoras": 0, "inicio": "2026-10-10T08:00:00"}]}
                                """.formatted(novaInternacao(), randomLong())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['itens[0].intervaloHoras']").exists());
    }

    @Test
    void horaForaDoPadraoRecebe400() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"internacaoId": %d,
                                 "itens": [{"medicacaoId": "%s", "intervaloHoras": 8, "inicio": "amanhã cedo"}]}
                                """.formatted(novaInternacao(), randomLong())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPorInternacaoDevolveSoAsDaquelaInternacao() throws Exception {
        long internacaoId = novaInternacao();
        criarPrescricao(internacaoId);
        criarPrescricao(novaInternacao());

        mockMvc.perform(get("/api/prescricoes").param("internacaoId", String.valueOf(internacaoId))
                        .header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].internacaoId").value(internacaoId));
    }

    @Test
    void buscarPorIdDevolveAPrescricaoComItens() throws Exception {
        String id = criarPrescricao(novaInternacao());

        mockMvc.perform(get("/api/prescricoes/" + id).header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    @Test
    void buscarInexistenteRecebe404() throws Exception {
        mockMvc.perform(get("/api/prescricoes/999999").header("Authorization", VET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idQueNaoENumeroRecebe400() throws Exception {
        mockMvc.perform(get("/api/prescricoes/abc").header("Authorization", VET))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desativarArquivaAPrescricaoSemApagar() throws Exception {
        String id = criarPrescricao(novaInternacao());

        mockMvc.perform(patch("/api/prescricoes/" + id + "/desativar").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/prescricoes/" + id + "/desativar").header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // continua listada, agora marcada como inativa
        mockMvc.perform(get("/api/prescricoes/" + id).header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }
}